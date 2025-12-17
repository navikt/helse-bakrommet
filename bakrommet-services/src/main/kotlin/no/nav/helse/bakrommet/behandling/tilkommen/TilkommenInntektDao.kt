package no.nav.helse.bakrommet.behandling.tilkommen

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.STATUS_UNDER_BEHANDLING_STR
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.infrastruktur.db.tilPgJson
import no.nav.helse.bakrommet.util.objectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

data class TilkommenInntektDbRecord(
    val id: UUID,
    val behandlingId: UUID,
    val tilkommenInntekt: TilkommenInntekt,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
)

enum class TilkommenInntektYrkesaktivitetType {
    VIRKSOMHET,
    PRIVATPERSON,
    NÆRINGSDRIVENDE,
}

data class TilkommenInntekt(
    val ident: String,
    val yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektForPerioden: BigDecimal,
    val notatTilBeslutter: String,
    val ekskluderteDager: List<LocalDate>,
)

interface TilkommenInntektDao {
    fun opprett(tilkommenInntektDbRecord: TilkommenInntektDbRecord): TilkommenInntektDbRecord

    fun hentForBehandling(behandlingId: UUID): List<TilkommenInntektDbRecord>

    fun oppdater(
        id: UUID,
        tilkommenInntekt: TilkommenInntekt,
    ): TilkommenInntektDbRecord

    fun slett(
        behandlingId: UUID,
        id: UUID,
    )

    fun hent(id: UUID): TilkommenInntektDbRecord?

    fun finnTilkommenInntektForBehandlinger(map: List<UUID>): List<TilkommenInntektDbRecord>
}

private val verifiserOppdatert: (Int) -> Unit = {
    if (it == 0) {
        throw KunneIkkeOppdatereDbException("Tilkommen inntekt kunne ikke oppdateres")
    }
}

private const val AND_ER_UNDER_BEHANDLING = "AND (select status from behandling where behandling.id = tilkommen_inntekt.behandling_id) = '$STATUS_UNDER_BEHANDLING_STR'"
private const val WHERE_ER_UNDER_BEHANDLING_FOR_INSERT = "WHERE EXISTS (select 1 from behandling where behandling.id = :behandling_id and status = '$STATUS_UNDER_BEHANDLING_STR')"

class TilkommenInntektDaoPg private constructor(
    private val db: QueryRunner,
) : TilkommenInntektDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun opprett(tilkommenInntektDbRecord: TilkommenInntektDbRecord): TilkommenInntektDbRecord {
        db
            .update(
                """
                insert into tilkommen_inntekt
                    (id, behandling_id, tilkommen_inntekt, opprettet, opprettet_av_nav_ident)
                select
                    :id, :behandling_id, :tilkommen_inntekt, :opprettet, :opprettet_av_nav_ident
                $WHERE_ER_UNDER_BEHANDLING_FOR_INSERT
                """.trimIndent(),
                "id" to tilkommenInntektDbRecord.id,
                "behandling_id" to tilkommenInntektDbRecord.behandlingId,
                "tilkommen_inntekt" to tilkommenInntektDbRecord.tilkommenInntekt.tilPgJson(),
                "opprettet" to tilkommenInntektDbRecord.opprettet,
                "opprettet_av_nav_ident" to tilkommenInntektDbRecord.opprettetAvNavIdent,
            ).also(verifiserOppdatert)
        return hent(tilkommenInntektDbRecord.id)!!
    }

    override fun hentForBehandling(behandlingId: UUID): List<TilkommenInntektDbRecord> =
        db.list(
            """
            select id, behandling_id, tilkommen_inntekt, opprettet, opprettet_av_nav_ident
            from tilkommen_inntekt
            where behandling_id = :behandling_id
            order by opprettet
            """.trimIndent(),
            "behandling_id" to behandlingId,
            mapper = ::tilkommenInntektFraRad,
        )

    override fun oppdater(
        id: UUID,
        tilkommenInntekt: TilkommenInntekt,
    ): TilkommenInntektDbRecord {
        db
            .update(
                """
                update tilkommen_inntekt
                   set tilkommen_inntekt = :tilkommen_inntekt
                 where id = :id
                 $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to id,
                "tilkommen_inntekt" to tilkommenInntekt.tilPgJson(),
            ).also(verifiserOppdatert)
        return hent(id)!!
    }

    override fun slett(
        behandlingId: UUID,
        id: UUID,
    ) {
        db
            .update(
                """
                delete from tilkommen_inntekt
                where id = :id
                and behandling_id = :behandling_id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to id,
                "behandling_id" to behandlingId,
            ).also(verifiserOppdatert)
    }

    override fun hent(id: UUID): TilkommenInntektDbRecord? =
        db.single(
            """
            select id, behandling_id, tilkommen_inntekt, opprettet, opprettet_av_nav_ident
            from tilkommen_inntekt
            where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::tilkommenInntektFraRad,
        )

    override fun finnTilkommenInntektForBehandlinger(map: List<UUID>): List<TilkommenInntektDbRecord> {
        if (map.isEmpty()) return emptyList()
        val params = map.mapIndexed { i, id -> "p$i" to id }
        val placeholderList = params.joinToString(",") { ":${it.first}" }
        return db.list(
            """
            select id, behandling_id, tilkommen_inntekt, opprettet, opprettet_av_nav_ident
            from tilkommen_inntekt
            where behandling_id IN ($placeholderList)
            """.trimIndent(),
            *params.toTypedArray(),
            mapper = ::tilkommenInntektFraRad,
        )
    }

    private fun tilkommenInntektFraRad(row: Row) =
        TilkommenInntektDbRecord(
            id = row.uuid("id"),
            behandlingId = row.uuid("behandling_id"),
            tilkommenInntekt =
                objectMapper.readValue(
                    row.string("tilkommen_inntekt"),
                    TilkommenInntekt::class.java,
                ),
            opprettet = row.offsetDateTime("opprettet"),
            opprettetAvNavIdent = row.string("opprettet_av_nav_ident"),
        )
}
