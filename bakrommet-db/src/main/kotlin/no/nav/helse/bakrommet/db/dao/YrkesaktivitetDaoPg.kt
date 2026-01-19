package no.nav.helse.bakrommet.db.dao

import com.fasterxml.jackson.core.type.TypeReference
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.dagoversikt.tilDagoversikt
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.tilPgJson
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.somListe
import java.time.OffsetDateTime
import java.util.*
import javax.sql.DataSource

private val verifiserOppdatert: (Int) -> Unit = {
    if (it == 0) {
        throw KunneIkkeOppdatereDbException("Yrkesaktivitet kunne ikke oppdateres")
    }
}

private const val AND_ER_UNDER_BEHANDLING = "AND (select status from behandling where behandling.id = yrkesaktivitet.behandling_id) = '${STATUS_UNDER_BEHANDLING_STR}'"
private const val WHERE_ER_UNDER_BEHANDLING_FOR_INSERT = "WHERE EXISTS (select 1 from behandling where behandling.id = :behandling_id and status = '${STATUS_UNDER_BEHANDLING_STR}')"

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

    override fun hentYrkesaktivitet(id: UUID): LegacyYrkesaktivitet? =
        hentYrkesaktivitetDbRecord(id)?.let { dbRecord ->
            LegacyYrkesaktivitet(
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

    override fun hentYrkesaktiviteter(periode: BehandlingDbRecord): List<LegacyYrkesaktivitet> =
        hentYrkesaktiviteterDbRecord(periode).map {
            it.tilYrkesaktivitet()
        }

    override fun hentYrkesaktiviteterDbRecord(periode: BehandlingDbRecord): List<YrkesaktivitetDbRecord> = hentYrkesaktiviteterDbRecord(periode.id)

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
            kategorisering =
                objectMapper.readValue(
                    row.string("kategorisering"),
                    YrkesaktivitetKategorisering::class.java,
                ),
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
                        object : TypeReference<List<Refusjonsperiode>>() {},
                    )
                },
        )

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
        legacyYrkesaktivitet: LegacyYrkesaktivitet,
        request: InntektRequest,
    ): YrkesaktivitetDbRecord {
        db
            .update(
                """
                update yrkesaktivitet set inntekt_request = :inntekt_request where id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to legacyYrkesaktivitet.id,
                "inntekt_request" to request.tilPgJson(),
            ).also(verifiserOppdatert)
        return hentYrkesaktivitetDbRecord(legacyYrkesaktivitet.id)!!
    }

    override fun oppdaterInntektData(
        legacyYrkesaktivitet: LegacyYrkesaktivitet,
        inntektData: InntektData,
    ): YrkesaktivitetDbRecord {
        db
            .update(
                """
                update yrkesaktivitet set inntekt_data = :inntekt_data where id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to legacyYrkesaktivitet.id,
                "inntekt_data" to inntektData.tilPgJson(),
            ).also(verifiserOppdatert)
        return hentYrkesaktivitetDbRecord(legacyYrkesaktivitet.id)!!
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
                    kategorisering =
                        objectMapper.readValue(
                            row.string("kategorisering"),
                            YrkesaktivitetKategorisering::class.java,
                        ),
                    behandlingId = row.uuid("behandling_id"),
                )
            },
        )
    }
}
