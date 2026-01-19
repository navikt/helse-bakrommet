package no.nav.helse.bakrommet.db.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.DbDagoversikt
import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.DbInntektData
import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.DbInntektRequest
import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.DbPerioder
import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.DbRefusjonsperiode
import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.DbYrkesaktivitet
import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.DbYrkesaktivitetKategorisering
import no.nav.helse.bakrommet.db.tilPgJson
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetId
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.repository.YrkesaktivitetRepository
import no.nav.helse.bakrommet.somListe
import java.util.UUID
import javax.sql.DataSource

class PgYrkesaktivitetRepository(
    private val queryRunner: QueryRunner,
) : YrkesaktivitetRepository {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun finn(behandlingId: BehandlingId): List<Yrkesaktivitet> =
        queryRunner
            .list(
                """
                select * from yrkesaktivitet where behandling_id = :behandling_id
                """.trimIndent(),
                "behandling_id" to behandlingId.value,
            ) {
                it.yrkesaktivitetFraRow()
            }.map {
                it.toYrkesaktivitet()
            }

    override fun finn(yrkesaktivitetId: YrkesaktivitetId): Yrkesaktivitet? =
        queryRunner
            .single(
                """
                select * from yrkesaktivitet where id = :id
                """.trimIndent(),
                "id" to yrkesaktivitetId.value,
            ) {
                it.yrkesaktivitetFraRow().toYrkesaktivitet()
            }

    override fun slett(yrkesaktivitetId: YrkesaktivitetId) {
        queryRunner.update(
            """
            delete from yrkesaktivitet where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitetId.value,
        )
    }

    override fun lagre(yrkesaktivitet: Yrkesaktivitet) {
        val record: DbYrkesaktivitet = yrkesaktivitet.toDbRecord()
        queryRunner.update(
            """
            insert into yrkesaktivitet
                (id, kategorisering, kategorisering_generert,
                dagoversikt, dagoversikt_generert,
                behandling_id, opprettet, generert_fra_dokumenter, perioder, inntekt_data, refusjon)
            values(
                :id, :kategorisering, :kategorisering_generert,
                :dagoversikt, :dagoversikt_generert,
                :behandling_id, :opprettet, :generert_fra_dokumenter, :perioder, :inntekt_data, :refusjon
            )
            on conflict (id) do update
                set kategorisering = excluded.kategorisering,
                    kategorisering_generert = excluded.kategorisering_generert,
                    dagoversikt = excluded.dagoversikt,
                    dagoversikt_generert = excluded.dagoversikt_generert,
                    perioder = excluded.perioder,
                    inntekt_data = excluded.inntekt_data,
                    refusjon = excluded.refusjon
            """.trimIndent(),
            "id" to record.id,
            "kategorisering" to record.kategorisering.tilPgJson(),
            "kategorisering_generert" to record.kategoriseringGenerert?.tilPgJson(),
            "dagoversikt" to record.dagoversikt?.tilPgJson(),
            "dagoversikt_generert" to record.dagoversiktGenerert?.tilPgJson(),
            "behandling_id" to record.behandlingId,
            "opprettet" to record.opprettet,
            "generert_fra_dokumenter" to record.generertFraDokumenter.tilPgJson(),
            "perioder" to record.perioder?.tilPgJson(),
            "inntekt_data" to record.inntektData?.tilPgJson(),
            "refusjon" to record.refusjon?.tilPgJson(),
        )
    }

    private fun Yrkesaktivitet.toDbRecord(): DbYrkesaktivitet =
        DbYrkesaktivitet(
            id = id.value,
            kategorisering = kategorisering.toDb(),
            kategoriseringGenerert = kategoriseringGenerert?.toDb(),
            dagoversikt = dagoversikt?.toDb(),
            dagoversiktGenerert = dagoversiktGenerert?.toDb(),
            behandlingId = behandlingId.value,
            opprettet = opprettet,
            generertFraDokumenter = generertFraDokumenter,
            perioder = perioder?.toDb(),
            inntektRequest = inntektRequest?.toDbInntektRequest(),
            inntektData = inntektData?.toDb(),
            refusjon = refusjon?.map { it.toDb() },
        )

    private fun DbYrkesaktivitet.toYrkesaktivitet(): Yrkesaktivitet =
        Yrkesaktivitet(
            id = YrkesaktivitetId(id),
            kategorisering = kategorisering.toDomain(),
            kategoriseringGenerert = kategoriseringGenerert?.toDomain(),
            dagoversikt = dagoversikt?.toDomain(),
            dagoversiktGenerert = dagoversiktGenerert?.toDomain(),
            behandlingId = BehandlingId(behandlingId),
            opprettet = opprettet,
            generertFraDokumenter = generertFraDokumenter,
            perioder = perioder?.toDomain(),
            inntektRequest = inntektRequest?.toDomain(),
            inntektData = inntektData?.toDomain(),
            refusjon = refusjon?.map { it.toDomain() },
        )

    private fun Row.yrkesaktivitetFraRow() =
        DbYrkesaktivitet(
            id = uuid("id"),
            kategorisering =
                objectMapper.readValue(
                    string("kategorisering"),
                    DbYrkesaktivitetKategorisering::class.java,
                ),
            kategoriseringGenerert =
                stringOrNull("kategorisering_generert")?.let {
                    objectMapper.readValue(it, DbYrkesaktivitetKategorisering::class.java)
                },
            dagoversikt = stringOrNull("dagoversikt")?.tilDagoversikt(),
            dagoversiktGenerert = stringOrNull("dagoversikt_generert")?.tilDagoversikt(),
            behandlingId = uuid("behandling_id"),
            opprettet = offsetDateTime("opprettet"),
            generertFraDokumenter =
                stringOrNull("generert_fra_dokumenter")
                    ?.somListe<UUID>() ?: emptyList(),
            perioder = stringOrNull("perioder")?.let { objectMapper.readValue(it, DbPerioder::class.java) },
            inntektRequest =
                stringOrNull("inntekt_request")
                    ?.let { objectMapper.readValue(it, DbInntektRequest::class.java) },
            inntektData = stringOrNull("inntekt_data")?.let { objectMapper.readValue(it, DbInntektData::class.java) },
            refusjon =
                stringOrNull("refusjon")?.let {
                    objectMapper.readValue(
                        it,
                        object : TypeReference<List<DbRefusjonsperiode>>() {},
                    )
                },
        )

    private fun String?.tilDagoversikt(): DbDagoversikt? {
        if (this == null) return null
        return try {
            objectMapper.readValue(this)
        } catch (e: Exception) {
            throw RuntimeException("feil ved parsing av dagoversikt", e)
        }
    }
}
