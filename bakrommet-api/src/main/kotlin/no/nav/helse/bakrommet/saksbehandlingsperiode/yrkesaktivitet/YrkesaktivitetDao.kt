package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.tilDagoversikt
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.util.*
import no.nav.helse.dto.InntektbeløpDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

// Intensjon om at denne kun lever i daoen
data class YrkesaktivitetDbRecord(
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
    val inntektData: InntektData? = null,
    val refusjonsdata: List<Refusjonsperiode>? = null,
)

data class Refusjonsperiode(
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: InntektbeløpDto.MånedligDouble,
)

fun YrkesaktivitetDbRecord.tilYrkesaktivitet(): Yrkesaktivitet =
    Yrkesaktivitet(
        id = this.id,
        kategorisering = this.kategorisering,
        kategoriseringGenerert = this.kategoriseringGenerert,
        dagoversikt = this.dagoversikt,
        dagoversiktGenerert = this.dagoversiktGenerert,
        saksbehandlingsperiodeId = this.saksbehandlingsperiodeId,
        opprettet = this.opprettet,
        generertFraDokumenter = this.generertFraDokumenter,
        perioder = this.perioder,
        inntektRequest = this.inntektRequest,
        inntektData = this.inntektData,
        refusjonsdata = this.refusjonsdata,
    )

fun Yrkesaktivitet.tilYrkesaktivitetDbRecord(): YrkesaktivitetDbRecord =
    YrkesaktivitetDbRecord(
        id = this.id,
        kategorisering = this.kategorisering,
        kategoriseringGenerert = this.kategoriseringGenerert,
        dagoversikt = this.dagoversikt,
        dagoversiktGenerert = this.dagoversiktGenerert,
        saksbehandlingsperiodeId = this.saksbehandlingsperiodeId,
        opprettet = this.opprettet,
        generertFraDokumenter = this.generertFraDokumenter,
        perioder = this.perioder,
        inntektRequest = this.inntektRequest,
        inntektData = this.inntektData,
        refusjonsdata = this.refusjonsdata,
    )

interface YrkesaktivitetDao {
    fun opprettYrkesaktivitet(
        id: UUID,
        kategorisering: YrkesaktivitetKategorisering,
        dagoversikt: List<Dag>?,
        saksbehandlingsperiodeId: UUID,
        opprettet: OffsetDateTime,
        generertFraDokumenter: List<UUID>,
        perioder: Perioder?,
        inntektData: InntektData?,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord

    fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord?

    fun hentYrkesaktivitet(id: UUID): Yrkesaktivitet?

    fun hentYrkesaktiviteter(periode: Saksbehandlingsperiode): List<Yrkesaktivitet>

    fun hentYrkesaktiviteterDbRecord(periode: Saksbehandlingsperiode): List<YrkesaktivitetDbRecord>

    fun oppdaterKategoriseringOgSlettInntektData(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        kategorisering: YrkesaktivitetKategorisering,
    ): YrkesaktivitetDbRecord

    fun oppdaterDagoversikt(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        oppdatertDagoversikt: List<Dag>,
    ): YrkesaktivitetDbRecord

    fun oppdaterPerioder(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        perioder: Perioder?,
    ): YrkesaktivitetDbRecord

    fun slettYrkesaktivitet(id: UUID)

    fun oppdaterInntektrequest(
        yrkesaktivitet: Yrkesaktivitet,
        request: InntektRequest,
    ): YrkesaktivitetDbRecord

    fun oppdaterInntektData(
        yrkesaktivitet: Yrkesaktivitet,
        inntektData: InntektData,
    ): YrkesaktivitetDbRecord

    fun oppdaterRefusjonsdata(
        yrkesaktivitet: Yrkesaktivitet,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord
}

class YrkesaktivitetDaoPg private constructor(
    private val db: QueryRunner,
) : YrkesaktivitetDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    /**
     * Oppretter yrkesaktivitet med type-sikker kategorisering.
     */
    override fun opprettYrkesaktivitet(
        id: UUID,
        kategorisering: YrkesaktivitetKategorisering,
        dagoversikt: List<Dag>?,
        saksbehandlingsperiodeId: UUID,
        opprettet: OffsetDateTime,
        generertFraDokumenter: List<UUID>,
        perioder: Perioder?,
        inntektData: InntektData?,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord {
        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = id,
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = dagoversikt,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = saksbehandlingsperiodeId,
                opprettet = opprettet,
                generertFraDokumenter = generertFraDokumenter,
                perioder = perioder,
                inntektData = inntektData,
                refusjonsdata = refusjonsdata,
            )
        db.update(
            """
            insert into yrkesaktivitet
                (id, kategorisering, kategorisering_generert,
                dagoversikt, dagoversikt_generert,
                saksbehandlingsperiode_id, opprettet, generert_fra_dokumenter, perioder, inntekt_data, refusjon_data)
            values
                (:id, :kategorisering, :kategorisering_generert,
                :dagoversikt, :dagoversikt_generert,
                :saksbehandlingsperiode_id, :opprettet, :generert_fra_dokumenter, :perioder, :inntekt_data, :refusjon_data)
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
            "inntekt_data" to yrkesaktivitetDbRecord.inntektData?.serialisertTilString(),
            "refusjon_data" to yrkesaktivitetDbRecord.refusjonsdata?.serialisertTilString(),
        )
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    override fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord? =
        db.single(
            """
            select *, inntekt_request, inntekt_data, refusjon_data from yrkesaktivitet where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::yrkesaktivitetFraRow,
        )

    override fun hentYrkesaktivitet(id: UUID): Yrkesaktivitet? =
        hentYrkesaktivitetDbRecord(id)?.let { dbRecord ->
            Yrkesaktivitet(
                id = dbRecord.id,
                kategorisering = dbRecord.kategorisering,
                kategoriseringGenerert = dbRecord.kategoriseringGenerert,
                dagoversikt = dbRecord.dagoversikt,
                dagoversiktGenerert = dbRecord.dagoversiktGenerert,
                saksbehandlingsperiodeId = dbRecord.saksbehandlingsperiodeId,
                opprettet = dbRecord.opprettet,
                generertFraDokumenter = dbRecord.generertFraDokumenter,
                perioder = dbRecord.perioder,
                inntektRequest = dbRecord.inntektRequest,
                inntektData = dbRecord.inntektData,
                refusjonsdata = dbRecord.refusjonsdata,
            )
        }

    override fun hentYrkesaktiviteter(periode: Saksbehandlingsperiode): List<Yrkesaktivitet> =
        hentYrkesaktiviteterDbRecord(periode).map {
            it.tilYrkesaktivitet()
        }

    override fun hentYrkesaktiviteterDbRecord(periode: Saksbehandlingsperiode): List<YrkesaktivitetDbRecord> =
        db.list(
            """
            select *, inntekt_request, inntekt_data, refusjon_data from yrkesaktivitet where saksbehandlingsperiode_id = :behandling_id
            """.trimIndent(),
            "behandling_id" to periode.id,
            mapper = ::yrkesaktivitetFraRow,
        )

    private fun yrkesaktivitetFraRow(row: Row) =
        YrkesaktivitetDbRecord(
            id = row.uuid("id"),
            kategorisering = objectMapper.readValue(row.string("kategorisering"), YrkesaktivitetKategorisering::class.java),
            kategoriseringGenerert =
                row.stringOrNull("kategorisering_generert")?.let {
                    objectMapper.readValue(it, YrkesaktivitetKategorisering::class.java)
                },
            dagoversikt = row.stringOrNull("dagoversikt")?.tilDagoversikt(),
            dagoversiktGenerert = row.stringOrNull("dagoversikt_generert")?.tilDagoversikt(),
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            opprettet = row.offsetDateTime("opprettet"),
            generertFraDokumenter =
                row
                    .stringOrNull("generert_fra_dokumenter")
                    ?.somListe<UUID>() ?: emptyList(),
            perioder = row.stringOrNull("perioder")?.let { objectMapper.readValue(it, Perioder::class.java) },
            inntektRequest =
                row
                    .stringOrNull("inntekt_request")
                    ?.let { objectMapper.readValue(it, InntektRequest::class.java) },
            inntektData = row.stringOrNull("inntekt_data")?.let { objectMapper.readValue(it, InntektData::class.java) },
            refusjonsdata =
                row.stringOrNull("refusjon_data")?.let {
                    objectMapper.readValue(
                        it,
                        object : com.fasterxml.jackson.core.type.TypeReference<List<Refusjonsperiode>>() {},
                    )
                },
        )

    /**
     * Oppdaterer kategorisering med type-sikker sealed class.
     */
    override fun oppdaterKategoriseringOgSlettInntektData(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        kategorisering: YrkesaktivitetKategorisering,
    ): YrkesaktivitetDbRecord {
        db.update(
            """
            update yrkesaktivitet set kategorisering = :kategorisering, inntekt_data=null, inntekt_request=null where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitetDbRecord.id,
            "kategorisering" to kategorisering.serialisertTilString(),
        )
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    override fun oppdaterDagoversikt(
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

    override fun oppdaterPerioder(
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

    override fun slettYrkesaktivitet(id: UUID) {
        db.update(
            """
            delete from yrkesaktivitet where id = :id
            """.trimIndent(),
            "id" to id,
        )
    }

    override fun oppdaterInntektrequest(
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

    override fun oppdaterInntektData(
        yrkesaktivitet: Yrkesaktivitet,
        inntektData: InntektData,
    ): YrkesaktivitetDbRecord {
        db.update(
            """
            update yrkesaktivitet set inntekt_data = :inntekt_data where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitet.id,
            "inntekt_data" to inntektData.serialisertTilString(),
        )
        return hentYrkesaktivitetDbRecord(yrkesaktivitet.id)!!
    }

    override fun oppdaterRefusjonsdata(
        yrkesaktivitet: Yrkesaktivitet,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord {
        db.update(
            """
            update yrkesaktivitet set refusjon_data = :refusjon_data where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitet.id,
            "refusjon_data" to refusjonsdata?.serialisertTilString(),
        )
        return hentYrkesaktivitetDbRecord(yrkesaktivitet.id)!!
    }
}
