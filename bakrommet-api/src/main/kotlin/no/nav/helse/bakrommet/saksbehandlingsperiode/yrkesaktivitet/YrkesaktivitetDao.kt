package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.tilDagoversikt
import no.nav.helse.bakrommet.util.*
import no.nav.helse.hendelser.Periode
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

// Intensjon om at denne kun lever i daoen
data class YrkesaktivitetDbRecord(
    val id: UUID,
    val kategorisering: Map<String, String>,
    val kategoriseringGenerert: Map<String, String>?,
    val dagoversikt: List<Dag>?,
    val dagoversiktGenerert: List<Dag>?,
    val saksbehandlingsperiodeId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder? = null,
    val inntektRequest: InntektRequest? = null,
) {
    fun hentPerioderForType(periodetype: Periodetype): List<Periode> =
        if (this.perioder?.type == periodetype) {
            this.perioder.perioder.map { Periode(it.fom, it.tom) }
        } else {
            emptyList()
        }
}

data class Yrkesaktivitet(
    val id: UUID,
    val kategorisering: YrkesaktivitetKategorisering,
    val kategoriseringGenerert: YrkesaktivitetKategorisering?,
    val dagoversikt: List<Dag>?,
    val dagoversiktGenerert: List<Dag>?,
    val saksbehandlingsperiodeId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder? = null,
    val inntektRequest: InntektRequest? = null,
)

class YrkesaktivitetDao private constructor(
    private val db: QueryRunner,
) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    /**
     * Oppretter yrkesaktivitet med type-sikker kategorisering.
     * Mapper sealed class til Map internt før lagring.
     */
    fun opprettYrkesaktivitet(
        id: UUID,
        kategorisering: YrkesaktivitetKategorisering,
        dagoversikt: List<Dag>?,
        saksbehandlingsperiodeId: UUID,
        opprettet: OffsetDateTime = OffsetDateTime.now(),
        generertFraDokumenter: List<UUID> = emptyList(),
        perioder: Perioder? = null,
    ): YrkesaktivitetDbRecord {
        val kategoriseringMap = YrkesaktivitetKategoriseringMapper.toMap(kategorisering)
        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = id,
                kategorisering = kategoriseringMap,
                kategoriseringGenerert = null,
                dagoversikt = dagoversikt,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = saksbehandlingsperiodeId,
                opprettet = opprettet,
                generertFraDokumenter = generertFraDokumenter,
                perioder = perioder,
            )
        db.update(
            """
            insert into yrkesaktivitet
                (id, kategorisering, kategorisering_generert,
                dagoversikt, dagoversikt_generert,
                saksbehandlingsperiode_id, opprettet, generert_fra_dokumenter, perioder)
            values
                (:id, :kategorisering, :kategorisering_generert,
                :dagoversikt, :dagoversikt_generert,
                :saksbehandlingsperiode_id, :opprettet, :generert_fra_dokumenter, :perioder)
            """.trimIndent(),
            "id" to yrkesaktivitetDbRecord.id,
            "kategorisering" to yrkesaktivitetDbRecord.kategorisering.serialisertTilString(),
            "kategorisering_generert" to yrkesaktivitetDbRecord.kategoriseringGenerert?.serialisertTilString(),
            "dagoversikt" to yrkesaktivitetDbRecord.dagoversikt?.serialisertTilString(),
            "dagoversikt_generert" to yrkesaktivitetDbRecord.dagoversiktGenerert?.serialisertTilString(),
            "saksbehandlingsperiode_id" to yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
            "opprettet" to yrkesaktivitetDbRecord.opprettet,
            "generert_fra_dokumenter" to yrkesaktivitetDbRecord.generertFraDokumenter.serialisertTilString(),
            "perioder" to yrkesaktivitetDbRecord.perioder?.serialisertTilString(),
        )
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord? =
        db.single(
            """
            select *, inntekt_request from yrkesaktivitet where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::yrkesaktivitetFraRow,
        )

    fun hentYrkesaktivitet(id: UUID): Yrkesaktivitet? =
        hentYrkesaktivitetDbRecord(id)?.let { dbRecord ->
            Yrkesaktivitet(
                id = dbRecord.id,
                kategorisering = YrkesaktivitetKategoriseringMapper.fromMap(dbRecord.kategorisering),
                kategoriseringGenerert = dbRecord.kategoriseringGenerert?.let { YrkesaktivitetKategoriseringMapper.fromMap(it) },
                dagoversikt = dbRecord.dagoversikt,
                dagoversiktGenerert = dbRecord.dagoversiktGenerert,
                saksbehandlingsperiodeId = dbRecord.saksbehandlingsperiodeId,
                opprettet = dbRecord.opprettet,
                generertFraDokumenter = dbRecord.generertFraDokumenter,
                perioder = dbRecord.perioder,
                inntektRequest = dbRecord.inntektRequest,
            )
        }

    fun hentYrkesaktivitetFor(periode: Saksbehandlingsperiode): List<YrkesaktivitetDbRecord> =
        db.list(
            """
            select *, inntekt_request from yrkesaktivitet where saksbehandlingsperiode_id = :behandling_id
            """.trimIndent(),
            "behandling_id" to periode.id,
            mapper = ::yrkesaktivitetFraRow,
        )

    private fun yrkesaktivitetFraRow(row: Row) =
        YrkesaktivitetDbRecord(
            id = row.uuid("id"),
            kategorisering = row.string("kategorisering").asStringStringMap(),
            kategoriseringGenerert = row.stringOrNull("kategorisering_generert")?.asStringStringMap(),
            dagoversikt = row.stringOrNull("dagoversikt")?.tilDagoversikt(),
            dagoversiktGenerert = row.stringOrNull("dagoversikt_generert")?.tilDagoversikt(),
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            opprettet = row.offsetDateTime("opprettet"),
            generertFraDokumenter =
                row
                    .stringOrNull("generert_fra_dokumenter")
                    ?.somListe<UUID>() ?: emptyList(),
            perioder = row.stringOrNull("perioder")?.let { objectMapper.readValue(it, Perioder::class.java) },
            inntektRequest = row.stringOrNull("inntekt_request")?.let { objectMapper.readValue(it, InntektRequest::class.java) },
        )

    /**
     * Oppdaterer kategorisering med type-sikker sealed class.
     * Mapper til Map internt før lagring.
     */
    fun oppdaterKategorisering(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        kategorisering: YrkesaktivitetKategorisering,
    ): YrkesaktivitetDbRecord {
        val kategoriseringMap = YrkesaktivitetKategoriseringMapper.toMap(kategorisering)
        db.update(
            """
            update yrkesaktivitet set kategorisering = :kategorisering where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitetDbRecord.id,
            "kategorisering" to kategoriseringMap.serialisertTilString(),
        )
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    fun oppdaterDagoversikt(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        oppdatertDagoversikt: List<Dag>,
    ): YrkesaktivitetDbRecord {
        db.update(
            """
            update yrkesaktivitet set dagoversikt = :dagoversikt where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitetDbRecord.id,
            "dagoversikt" to oppdatertDagoversikt.serialisertTilString(),
        )
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    fun oppdaterPerioder(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        perioder: Perioder?,
    ): YrkesaktivitetDbRecord {
        db.update(
            """
            update yrkesaktivitet set perioder = :perioder where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitetDbRecord.id,
            "perioder" to perioder?.serialisertTilString(),
        )
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    fun slettYrkesaktivitet(id: UUID) {
        db.update(
            """
            delete from yrkesaktivitet where id = :id
            """.trimIndent(),
            "id" to id,
        )
    }

    fun oppdaterInntektrequest(
        yrkesaktivitet: Yrkesaktivitet,
        request: InntektRequest,
    ): YrkesaktivitetDbRecord {
        db.update(
            """
            update yrkesaktivitet set inntekt_request = :inntekt_request where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitet.id,
            "inntekt_request" to request.serialisertTilString(),
        )
        return hentYrkesaktivitetDbRecord(yrkesaktivitet.id)!!
    }
}
