package no.nav.helse.bakrommet.db.dao

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.*
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.infrastruktur.db.tilPgJson
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
import java.util.*
import javax.sql.DataSource

private val verifiserOppdatert: (Int) -> Unit = {
    if (it == 0) {
        throw KunneIkkeOppdatereDbException("Utbetalingsberegning kunne ikke oppdateres")
    }
}

private const val AND_ER_UNDER_BEHANDLING = "AND (select status from behandling where behandling.id = utbetalingsberegning.behandling_id) = '${STATUS_UNDER_BEHANDLING_STR}'"
private const val WHERE_ER_UNDER_BEHANDLING_FOR_INSERT = "WHERE EXISTS (select 1 from behandling where behandling.id = :behandling_id and status = '${STATUS_UNDER_BEHANDLING_STR}')"

class UtbetalingsberegningDaoPg private constructor(
    private val db: QueryRunner,
) : UtbetalingsberegningDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun settBeregning(
        behandlingId: UUID,
        beregning: BeregningResponse,
        saksbehandler: Bruker,
    ): BeregningResponse {
        // Konverter til InnDto format for lagring (slik at deserialisering fungerer)
        val beregningDataUtDto = beregning.beregningData.tilBeregningDataUtDto()
        val beregningDataInnDto = beregningDataUtDto.tilBeregningDataInnDto()

        // Sjekk om det finnes fra før
        val eksisterende = hentBeregning(behandlingId)

        if (eksisterende != null) {
            // Oppdater eksisterende
            db
                .update(
                    """
                    UPDATE utbetalingsberegning 
                    SET 
                        utbetalingsberegning_data = :utbetalingsberegning_data,
                        opprettet_av_nav_ident = :opprettet_av_nav_ident,
                        sist_oppdatert = NOW()
                    WHERE behandling_id = :behandling_id
                    $AND_ER_UNDER_BEHANDLING
                    """.trimIndent(),
                    "behandling_id" to behandlingId,
                    "utbetalingsberegning_data" to beregningDataInnDto.tilPgJson(),
                    "opprettet_av_nav_ident" to saksbehandler.navIdent,
                ).also(verifiserOppdatert)
        } else {
            // Opprett nytt
            db
                .update(
                    """
                    INSERT INTO utbetalingsberegning 
                        (id, behandling_id, utbetalingsberegning_data, opprettet, opprettet_av_nav_ident, sist_oppdatert)
                    SELECT 
                        :id, :behandling_id, :utbetalingsberegning_data, NOW(), :opprettet_av_nav_ident, NOW()
                    $WHERE_ER_UNDER_BEHANDLING_FOR_INSERT
                    """.trimIndent(),
                    "id" to beregning.id,
                    "behandling_id" to behandlingId,
                    "utbetalingsberegning_data" to beregningDataInnDto.tilPgJson(),
                    "opprettet_av_nav_ident" to saksbehandler.navIdent,
                ).also(verifiserOppdatert)
        }

        return hentBeregning(behandlingId)!!
    }

    override fun hentBeregning(behandlingId: UUID): BeregningResponse? =
        db.single(
            """
            SELECT * FROM utbetalingsberegning 
            WHERE behandling_id = :behandling_id
            """.trimIndent(),
            "behandling_id" to behandlingId,
            mapper = ::beregningFraRow,
        )

    override fun slettBeregning(
        behandlingId: UUID,
        failSilently: Boolean,
    ) {
        db
            .update(
                """
                DELETE FROM utbetalingsberegning 
                WHERE behandling_id = :behandling_id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "behandling_id" to behandlingId,
            ).also {
                if (!failSilently) {
                    verifiserOppdatert(it)
                }
            }
    }

    private fun beregningFraRow(row: Row): BeregningResponse {
        val beregningJson = row.string("utbetalingsberegning_data")
        val beregningData = objectMapper.readValue(beregningJson, BeregningDataInnDto::class.java)

        return BeregningResponse(
            id = row.uuid("id"),
            behandlingId = row.uuid("behandling_id"),
            beregningData = beregningData.tilBeregningData(),
            opprettet = row.offsetDateTime("opprettet").toString(),
            opprettetAv = row.string("opprettet_av_nav_ident"),
            sistOppdatert = row.offsetDateTime("sist_oppdatert").toString(),
        )
    }
}

private fun BeregningData.tilBeregningDataUtDto(): BeregningDataUtDto =
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

private fun BeregningResponse.tilBeregningResponseUtDto(): BeregningResponseUtDto =
    BeregningResponseUtDto(
        id = id,
        behandlingId = behandlingId,
        beregningData = beregningData.tilBeregningDataUtDto(),
        opprettet = opprettet,
        opprettetAv = opprettetAv,
        sistOppdatert = sistOppdatert,
    )

// Converter funksjoner for å konvertere UtDto til InnDto format for lagring
private fun InntektDto.tilDagligDouble(): `InntektbeløpDto`.DagligDouble = this.dagligDouble

private fun `ØkonomiUtDto`.tilØkonomiInnDto(): `ØkonomiInnDto` =
    `ØkonomiInnDto`(
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
