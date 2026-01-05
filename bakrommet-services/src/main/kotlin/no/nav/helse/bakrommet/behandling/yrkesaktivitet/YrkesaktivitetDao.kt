package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.STATUS_UNDER_BEHANDLING_STR
import no.nav.helse.bakrommet.behandling.dagoversikt.tilDagoversikt
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.infrastruktur.db.tilPgJson
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.somListe
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
    val dagoversikt: Dagoversikt?,
    val dagoversiktGenerert: Dagoversikt?,
    val behandlingId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder? = null,
    val inntektRequest: InntektRequest? = null,
    val inntektData: InntektData? = null,
    val refusjon: List<Refusjonsperiode>? = null,
)

data class YrkesaktivitetForenkletDbRecord(
    val id: UUID,
    val kategorisering: YrkesaktivitetKategorisering,
    val behandlingId: UUID,
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
        behandlingId = this.behandlingId,
        opprettet = this.opprettet,
        generertFraDokumenter = this.generertFraDokumenter,
        perioder = this.perioder,
        inntektRequest = this.inntektRequest,
        inntektData = this.inntektData,
        refusjon = this.refusjon,
    )

fun Yrkesaktivitet.tilYrkesaktivitetDbRecord(): YrkesaktivitetDbRecord =
    YrkesaktivitetDbRecord(
        id = this.id,
        kategorisering = this.kategorisering,
        kategoriseringGenerert = this.kategoriseringGenerert,
        dagoversikt = this.dagoversikt,
        dagoversiktGenerert = this.dagoversiktGenerert,
        behandlingId = this.behandlingId,
        opprettet = this.opprettet,
        generertFraDokumenter = this.generertFraDokumenter,
        perioder = this.perioder,
        inntektRequest = this.inntektRequest,
        inntektData = this.inntektData,
        refusjon = this.refusjon,
    )

interface YrkesaktivitetDao {
    fun opprettYrkesaktivitet(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
    ): YrkesaktivitetDbRecord =
        opprettYrkesaktivitet(
            id = yrkesaktivitetDbRecord.id,
            kategorisering = yrkesaktivitetDbRecord.kategorisering,
            dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
            behandlingId = yrkesaktivitetDbRecord.behandlingId,
            opprettet = yrkesaktivitetDbRecord.opprettet,
            generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
            perioder = yrkesaktivitetDbRecord.perioder,
            inntektData = yrkesaktivitetDbRecord.inntektData,
            refusjonsdata = yrkesaktivitetDbRecord.refusjon,
        )

    fun opprettYrkesaktivitet(
        id: UUID,
        kategorisering: YrkesaktivitetKategorisering,
        dagoversikt: Dagoversikt?,
        behandlingId: UUID,
        opprettet: OffsetDateTime,
        generertFraDokumenter: List<UUID>,
        perioder: Perioder?,
        inntektData: InntektData?,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord

    fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord?

    fun hentYrkesaktivitet(id: UUID): Yrkesaktivitet?

    fun hentYrkesaktiviteter(periode: Behandling): List<Yrkesaktivitet>

    fun hentYrkesaktiviteterDbRecord(periode: Behandling): List<YrkesaktivitetDbRecord>

    fun hentYrkesaktiviteterDbRecord(behandlingId: UUID): List<YrkesaktivitetDbRecord>

    fun oppdaterKategoriseringOgSlettInntektData(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        kategorisering: YrkesaktivitetKategorisering,
    ): YrkesaktivitetDbRecord

    fun oppdaterDagoversikt(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        oppdatertDagoversikt: Dagoversikt,
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

    fun oppdaterRefusjon(
        yrkesaktivitetID: UUID,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord

    fun finnYrkesaktiviteterForBehandlinger(map: List<UUID>): List<YrkesaktivitetForenkletDbRecord>
}

private val verifiserOppdatert: (Int) -> Unit = {
    if (it == 0) {
        throw KunneIkkeOppdatereDbException("Yrkesaktivitet kunne ikke oppdateres")
    }
}

private const val AND_ER_UNDER_BEHANDLING = "AND (select status from behandling where behandling.id = yrkesaktivitet.behandling_id) = '$STATUS_UNDER_BEHANDLING_STR'"
private const val WHERE_ER_UNDER_BEHANDLING_FOR_INSERT = "WHERE EXISTS (select 1 from behandling where behandling.id = :behandling_id and status = '$STATUS_UNDER_BEHANDLING_STR')"

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
        dagoversikt: Dagoversikt?,
        behandlingId: UUID,
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
                behandlingId = behandlingId,
                opprettet = opprettet,
                generertFraDokumenter = generertFraDokumenter,
                perioder = perioder,
                inntektData = inntektData,
                refusjon = refusjonsdata,
            )
        db
            .update(
                """
                insert into yrkesaktivitet
                    (id, kategorisering, kategorisering_generert,
                    dagoversikt, dagoversikt_generert,
                    behandling_id, opprettet, generert_fra_dokumenter, perioder, inntekt_data, refusjon)
                select
                    :id, :kategorisering, :kategorisering_generert,
                    :dagoversikt, :dagoversikt_generert,
                    :behandling_id, :opprettet, :generert_fra_dokumenter, :perioder, :inntekt_data, :refusjon
                $WHERE_ER_UNDER_BEHANDLING_FOR_INSERT
                """.trimIndent(),
                "id" to yrkesaktivitetDbRecord.id,
                "kategorisering" to yrkesaktivitetDbRecord.kategorisering.tilPgJson(),
                "kategorisering_generert" to yrkesaktivitetDbRecord.kategoriseringGenerert?.tilPgJson(),
                "dagoversikt" to yrkesaktivitetDbRecord.dagoversikt?.tilPgJson(),
                "dagoversikt_generert" to yrkesaktivitetDbRecord.dagoversiktGenerert?.tilPgJson(),
                "behandling_id" to yrkesaktivitetDbRecord.behandlingId,
                "opprettet" to yrkesaktivitetDbRecord.opprettet,
                "generert_fra_dokumenter" to yrkesaktivitetDbRecord.generertFraDokumenter.tilPgJson(),
                "perioder" to yrkesaktivitetDbRecord.perioder?.tilPgJson(),
                "inntekt_data" to yrkesaktivitetDbRecord.inntektData?.tilPgJson(),
                "refusjon" to yrkesaktivitetDbRecord.refusjon?.tilPgJson(),
            ).also(verifiserOppdatert)
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    override fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord? =
        db.single(
            """
            select *, inntekt_request, inntekt_data, refusjon from yrkesaktivitet where id = :id
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
                behandlingId = dbRecord.behandlingId,
                opprettet = dbRecord.opprettet,
                generertFraDokumenter = dbRecord.generertFraDokumenter,
                perioder = dbRecord.perioder,
                inntektRequest = dbRecord.inntektRequest,
                inntektData = dbRecord.inntektData,
                refusjon = dbRecord.refusjon,
            )
        }

    override fun hentYrkesaktiviteter(periode: Behandling): List<Yrkesaktivitet> =
        hentYrkesaktiviteterDbRecord(periode).map {
            it.tilYrkesaktivitet()
        }

    override fun hentYrkesaktiviteterDbRecord(periode: Behandling): List<YrkesaktivitetDbRecord> = hentYrkesaktiviteterDbRecord(periode.id)

    override fun hentYrkesaktiviteterDbRecord(behandlingId: UUID): List<YrkesaktivitetDbRecord> =
        db.list(
            """
            select *, inntekt_request, inntekt_data, refusjon from yrkesaktivitet where behandling_id = :behandling_id
            """.trimIndent(),
            "behandling_id" to behandlingId,
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
            behandlingId = row.uuid("behandling_id"),
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
            refusjon =
                row.stringOrNull("refusjon")?.let {
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
        db
            .update(
                """
                update yrkesaktivitet set kategorisering = :kategorisering, inntekt_data=null, inntekt_request=null where id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to yrkesaktivitetDbRecord.id,
                "kategorisering" to kategorisering.tilPgJson(),
            ).also(verifiserOppdatert)
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    override fun oppdaterDagoversikt(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        oppdatertDagoversikt: Dagoversikt,
    ): YrkesaktivitetDbRecord {
        db
            .update(
                """
                update yrkesaktivitet set dagoversikt = :dagoversikt where id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to yrkesaktivitetDbRecord.id,
                "dagoversikt" to oppdatertDagoversikt.tilPgJson(),
            ).also(verifiserOppdatert)
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    override fun oppdaterPerioder(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        perioder: Perioder?,
    ): YrkesaktivitetDbRecord {
        db
            .update(
                """
                update yrkesaktivitet set perioder = :perioder where id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to yrkesaktivitetDbRecord.id,
                "perioder" to perioder?.tilPgJson(),
            ).also(verifiserOppdatert)
        return hentYrkesaktivitetDbRecord(yrkesaktivitetDbRecord.id)!!
    }

    override fun slettYrkesaktivitet(id: UUID) {
        db
            .update(
                """
                delete from yrkesaktivitet where id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to id,
            ).also(verifiserOppdatert)
    }

    override fun oppdaterInntektrequest(
        yrkesaktivitet: Yrkesaktivitet,
        request: InntektRequest,
    ): YrkesaktivitetDbRecord {
        db
            .update(
                """
                update yrkesaktivitet set inntekt_request = :inntekt_request where id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to yrkesaktivitet.id,
                "inntekt_request" to request.tilPgJson(),
            ).also(verifiserOppdatert)
        return hentYrkesaktivitetDbRecord(yrkesaktivitet.id)!!
    }

    override fun oppdaterInntektData(
        yrkesaktivitet: Yrkesaktivitet,
        inntektData: InntektData,
    ): YrkesaktivitetDbRecord {
        db
            .update(
                """
                update yrkesaktivitet set inntekt_data = :inntekt_data where id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to yrkesaktivitet.id,
                "inntekt_data" to inntektData.tilPgJson(),
            ).also(verifiserOppdatert)
        return hentYrkesaktivitetDbRecord(yrkesaktivitet.id)!!
    }

    override fun oppdaterRefusjon(
        yrkesaktivitetID: UUID,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord {
        db
            .update(
                """
                update yrkesaktivitet set refusjon = :refusjon where id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to yrkesaktivitetID,
                "refusjon" to refusjonsdata?.tilPgJson(),
            ).also(verifiserOppdatert)
        return hentYrkesaktivitetDbRecord(yrkesaktivitetID)!!
    }

    override fun finnYrkesaktiviteterForBehandlinger(map: List<UUID>): List<YrkesaktivitetForenkletDbRecord> {
        if (map.isEmpty()) return emptyList()
        val params = map.mapIndexed { i, id -> "p$i" to id }
        val placeholderList = params.joinToString(",") { ":${it.first}" }
        return db.list(
            """
            select id, kategorisering, behandling_id from yrkesaktivitet where behandling_id IN ($placeholderList)
            """.trimIndent(),
            *params.toTypedArray(),
            mapper = { row ->
                YrkesaktivitetForenkletDbRecord(
                    id = row.uuid("id"),
                    kategorisering = objectMapper.readValue(row.string("kategorisering"), YrkesaktivitetKategorisering::class.java),
                    behandlingId = row.uuid("behandling_id"),
                )
            },
        )
    }
}
