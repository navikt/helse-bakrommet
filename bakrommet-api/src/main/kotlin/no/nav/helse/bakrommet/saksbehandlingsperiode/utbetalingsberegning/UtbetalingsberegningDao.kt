package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto
import no.nav.helse.dto.deserialisering.UtbetalingstidslinjeInnDto
import no.nav.helse.dto.deserialisering.ØkonomiInnDto
import no.nav.helse.dto.serialisering.UtbetalingsdagUtDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.dto.serialisering.ØkonomiUtDto
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.util.UUID
import javax.sql.DataSource

interface UtbetalingsberegningDao {
    fun settBeregning(
        saksbehandlingsperiodeId: UUID,
        beregning: BeregningResponse,
        saksbehandler: Bruker,
    ): BeregningResponse

    fun hentBeregning(saksbehandlingsperiodeId: UUID): BeregningResponse?

    fun slettBeregning(saksbehandlingsperiodeId: UUID)
}

class UtbetalingsberegningDaoPg private constructor(
    private val db: QueryRunner,
) : UtbetalingsberegningDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun settBeregning(
        saksbehandlingsperiodeId: UUID,
        beregning: BeregningResponse,
        saksbehandler: Bruker,
    ): BeregningResponse {
        // Konverter til InnDto format for lagring (slik at deserialisering fungerer)
        val beregningDataUtDto = beregning.beregningData.tilBeregningDataUtDto()
        val beregningDataInnDto = beregningDataUtDto.tilBeregningDataInnDto()
        val beregningJson = objectMapper.writeValueAsString(beregningDataInnDto)

        // Sjekk om det finnes fra før
        val eksisterende = hentBeregning(saksbehandlingsperiodeId)

        if (eksisterende != null) {
            // Oppdater eksisterende
            db.update(
                """
                UPDATE utbetalingsberegning 
                SET 
                    utbetalingsberegning_data = :utbetalingsberegning_data,
                    opprettet_av_nav_ident = :opprettet_av_nav_ident,
                    sist_oppdatert = NOW()
                WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
                """.trimIndent(),
                "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
                "utbetalingsberegning_data" to beregningJson,
                "opprettet_av_nav_ident" to saksbehandler.navIdent,
            )
        } else {
            // Opprett nytt
            db.update(
                """
                INSERT INTO utbetalingsberegning 
                    (id, saksbehandlingsperiode_id, utbetalingsberegning_data, opprettet, opprettet_av_nav_ident, sist_oppdatert)
                VALUES 
                    (:id, :saksbehandlingsperiode_id, :utbetalingsberegning_data, NOW(), :opprettet_av_nav_ident, NOW())
                """.trimIndent(),
                "id" to beregning.id,
                "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
                "utbetalingsberegning_data" to beregningJson,
                "opprettet_av_nav_ident" to saksbehandler.navIdent,
            )
        }

        return hentBeregning(saksbehandlingsperiodeId)!!
    }

    override fun hentBeregning(saksbehandlingsperiodeId: UUID): BeregningResponse? =
        db.single(
            """
            SELECT * FROM utbetalingsberegning 
            WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            mapper = ::beregningFraRow,
        )

    override fun slettBeregning(saksbehandlingsperiodeId: UUID) {
        db.update(
            """
            DELETE FROM utbetalingsberegning 
            WHERE saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
        )
    }

    private fun beregningFraRow(row: Row): BeregningResponse {
        val beregningJson = row.string("utbetalingsberegning_data")
        val beregningData = objectMapper.readValue(beregningJson, BeregningDataInnDto::class.java)

        return BeregningResponse(
            id = row.uuid("id"),
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            beregningData = beregningData.tilBeregningData(),
            opprettet = row.offsetDateTime("opprettet").toString(),
            opprettetAv = row.string("opprettet_av_nav_ident"),
            sistOppdatert = row.offsetDateTime("sist_oppdatert").toString(),
        )
    }
}

internal fun BeregningData.tilBeregningDataUtDto(): BeregningDataUtDto =
    BeregningDataUtDto(
        yrkesaktiviteter =
            yrkesaktiviteter.map {
                YrkesaktivitetUtbetalingsberegningUtDto(
                    yrkesaktivitetId = it.yrkesaktivitetId,
                    utbetalingstidslinje = it.utbetalingstidslinje.dto(),
                    dekningsgrad = it.dekningsgrad,
                )
            },
        spilleromOppdrag = spilleromOppdrag,
    )

private fun BeregningDataInnDto.tilBeregningData(): BeregningData =
    BeregningData(
        yrkesaktiviteter =
            yrkesaktiviteter.map {
                YrkesaktivitetUtbetalingsberegning(
                    yrkesaktivitetId = it.yrkesaktivitetId,
                    utbetalingstidslinje = Utbetalingstidslinje.gjenopprett(it.utbetalingstidslinje),
                    dekningsgrad = it.dekningsgrad,
                )
            },
        spilleromOppdrag = spilleromOppdrag,
    )

internal fun BeregningResponse.tilBeregningResponseUtDto(): BeregningResponseUtDto =
    BeregningResponseUtDto(
        id = id,
        saksbehandlingsperiodeId = saksbehandlingsperiodeId,
        beregningData = beregningData.tilBeregningDataUtDto(),
        opprettet = opprettet,
        opprettetAv = opprettetAv,
        sistOppdatert = sistOppdatert,
    )

// Converter funksjoner for å konvertere UtDto til InnDto format for lagring
private fun InntektDto.tilDagligDouble(): InntektbeløpDto.DagligDouble = this.dagligDouble

private fun ØkonomiUtDto.tilØkonomiInnDto(): ØkonomiInnDto =
    ØkonomiInnDto(
        grad = grad,
        totalGrad = totalGrad,
        utbetalingsgrad = utbetalingsgrad,
        arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp.tilDagligDouble(),
        aktuellDagsinntekt = aktuellDagsinntekt.tilDagligDouble(),
        inntektjustering = inntektjustering.tilDagligDouble(),
        dekningsgrad = dekningsgrad,
        arbeidsgiverbeløp = arbeidsgiverbeløp?.tilDagligDouble(),
        personbeløp = personbeløp?.tilDagligDouble(),
        reservertArbeidsgiverbeløp = reservertArbeidsgiverbeløp?.tilDagligDouble(),
        reservertPersonbeløp = reservertPersonbeløp?.tilDagligDouble(),
    )

private fun UtbetalingsdagUtDto.tilUtbetalingsdagInnDto(): UtbetalingsdagInnDto =
    when (this) {
        is UtbetalingsdagUtDto.NavDagDto -> UtbetalingsdagInnDto.NavDagDto(dato, økonomi.tilØkonomiInnDto())
        is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagDto -> UtbetalingsdagInnDto.ArbeidsgiverperiodeDagDto(dato, økonomi.tilØkonomiInnDto())
        is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagNavDto ->
            UtbetalingsdagInnDto.ArbeidsgiverperiodeDagNavDto(
                dato,
                økonomi.tilØkonomiInnDto(),
            )
        is UtbetalingsdagUtDto.NavHelgDagDto -> UtbetalingsdagInnDto.NavHelgDagDto(dato, økonomi.tilØkonomiInnDto())
        is UtbetalingsdagUtDto.ArbeidsdagDto -> UtbetalingsdagInnDto.ArbeidsdagDto(dato, økonomi.tilØkonomiInnDto())
        is UtbetalingsdagUtDto.FridagDto -> UtbetalingsdagInnDto.FridagDto(dato, økonomi.tilØkonomiInnDto())
        is UtbetalingsdagUtDto.AvvistDagDto -> UtbetalingsdagInnDto.AvvistDagDto(dato, økonomi.tilØkonomiInnDto(), begrunnelser)
        is UtbetalingsdagUtDto.ForeldetDagDto -> UtbetalingsdagInnDto.ForeldetDagDto(dato, økonomi.tilØkonomiInnDto())
        is UtbetalingsdagUtDto.UkjentDagDto -> UtbetalingsdagInnDto.UkjentDagDto(dato, økonomi.tilØkonomiInnDto())
        is UtbetalingsdagUtDto.VentetidsdagDto -> UtbetalingsdagInnDto.VentetidsdagDto(dato, økonomi.tilØkonomiInnDto())
    }

private fun UtbetalingstidslinjeUtDto.tilUtbetalingstidslinjeInnDto(): UtbetalingstidslinjeInnDto =
    UtbetalingstidslinjeInnDto(
        dager = dager.map { it.tilUtbetalingsdagInnDto() },
    )

private fun YrkesaktivitetUtbetalingsberegningUtDto.tilYrkesaktivitetUtbetalingsberegningInnDto(): YrkesaktivitetUtbetalingsberegningInnDto =
    YrkesaktivitetUtbetalingsberegningInnDto(
        yrkesaktivitetId = yrkesaktivitetId,
        utbetalingstidslinje = utbetalingstidslinje.tilUtbetalingstidslinjeInnDto(),
        dekningsgrad = dekningsgrad,
    )

private fun BeregningDataUtDto.tilBeregningDataInnDto(): BeregningDataInnDto =
    BeregningDataInnDto(
        yrkesaktiviteter = yrkesaktiviteter.map { it.tilYrkesaktivitetUtbetalingsberegningInnDto() },
        spilleromOppdrag = spilleromOppdrag,
    )
