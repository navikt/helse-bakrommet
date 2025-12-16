package no.nav.helse.bakrommet.behandling.sykepengegrunnlag

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.STATUS_UNDER_BEHANDLING_STR
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.util.objectMapper
import java.util.UUID
import javax.sql.DataSource

interface SykepengegrunnlagDao {
    fun lagreSykepengegrunnlag(
        sykepengegrunnlag: SykepengegrunnlagBase?,
        saksbehandler: Bruker,
        opprettetForBehandling: UUID,
    ): SykepengegrunnlagDbRecord

    fun finnSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord?

    fun hentSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord =
        finnSykepengegrunnlag(sykepengegrunnlagId)
            ?: throw IllegalArgumentException("Fant ikke sykepengegrunnlag for id $sykepengegrunnlagId")

    fun oppdaterSykepengegrunnlag(
        sykepengegrunnlagId: UUID,
        sykepengegrunnlag: SykepengegrunnlagBase?,
    ): SykepengegrunnlagDbRecord

    fun oppdaterSammenlikningsgrunnlag(
        sykepengegrunnlagId: UUID,
        sammenlikningsgrunnlag: Sammenlikningsgrunnlag?,
    ): SykepengegrunnlagDbRecord

    fun slettSykepengegrunnlag(sykepengegrunnlagId: UUID)

    fun settLåst(sykepengegrunnlagId: UUID)
}

private const val AND_ER_UNDER_BEHANDLING = "AND (select status from behandling where behandling.id = sykepengegrunnlag.opprettet_for_behandling) = '$STATUS_UNDER_BEHANDLING_STR'"
private const val WHERE_ER_UNDER_BEHANDLING_FOR_INSERT = "WHERE EXISTS (select 1 from behandling where behandling.id = :opprettet_for_behandling and status = '$STATUS_UNDER_BEHANDLING_STR')"

class SykepengegrunnlagDaoPg private constructor(
    private val db: QueryRunner,
) : SykepengegrunnlagDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun lagreSykepengegrunnlag(
        sykepengegrunnlag: SykepengegrunnlagBase?,
        saksbehandler: Bruker,
        opprettetForBehandling: UUID,
    ): SykepengegrunnlagDbRecord {
        val id = UUID.randomUUID()
        val nå = java.time.Instant.now()
        val sykepengegrunnlagJson = objectMapper.writeValueAsString(sykepengegrunnlag)

        db
            .update(
                """
                INSERT INTO sykepengegrunnlag (id, sykepengegrunnlag, opprettet_av_nav_ident, opprettet, oppdatert, opprettet_for_behandling, laast)
                SELECT :id, :sykepengegrunnlag, :opprettet_av_nav_ident, :opprettet, :oppdatert, :opprettet_for_behandling, false
                $WHERE_ER_UNDER_BEHANDLING_FOR_INSERT
                """.trimIndent(),
                "id" to id,
                "sykepengegrunnlag" to sykepengegrunnlagJson,
                "opprettet_av_nav_ident" to saksbehandler.navIdent,
                "opprettet" to nå,
                "oppdatert" to nå,
                "opprettet_for_behandling" to opprettetForBehandling,
            ).also(verifiserOppdatert)

        return finnSykepengegrunnlag(id)!!
    }

    override fun finnSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord? =
        db
            .list(
                """
                SELECT *
                FROM sykepengegrunnlag
                WHERE id = :id
                """.trimIndent(),
                "id" to sykepengegrunnlagId,
                mapper = ::sykepengegrunnlagFraRow,
            ).firstOrNull()

    override fun oppdaterSykepengegrunnlag(
        sykepengegrunnlagId: UUID,
        sykepengegrunnlag: SykepengegrunnlagBase?,
    ): SykepengegrunnlagDbRecord {
        val nå = java.time.Instant.now()
        val sykepengegrunnlagJson = sykepengegrunnlag?.let { objectMapper.writeValueAsString(it) }

        db
            .update(
                """
                UPDATE sykepengegrunnlag 
                SET sykepengegrunnlag = :sykepengegrunnlag, oppdatert = :oppdatert
                WHERE id = :id and not laast
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to sykepengegrunnlagId,
                "sykepengegrunnlag" to sykepengegrunnlagJson,
                "oppdatert" to nå,
            ).also(verifiserOppdatert)

        return finnSykepengegrunnlag(sykepengegrunnlagId)!!
    }

    override fun oppdaterSammenlikningsgrunnlag(
        sykepengegrunnlagId: UUID,
        sammenlikningsgrunnlag: Sammenlikningsgrunnlag?,
    ): SykepengegrunnlagDbRecord {
        require(finnSykepengegrunnlag(sykepengegrunnlagId)!!.sammenlikningsgrunnlag == null)

        val nå = java.time.Instant.now()
        val sammenlikningsgrunnlagJson = sammenlikningsgrunnlag?.let { objectMapper.writeValueAsString(it) }

        db
            .update(
                """
                UPDATE sykepengegrunnlag 
                SET sammenlikningsgrunnlag = :sammenlikningsgrunnlag, oppdatert = :oppdatert
                WHERE id = :id and not laast
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to sykepengegrunnlagId,
                "sammenlikningsgrunnlag" to sammenlikningsgrunnlagJson,
                "oppdatert" to nå,
            ).also(verifiserOppdatert)

        return finnSykepengegrunnlag(sykepengegrunnlagId)!!
    }

    override fun slettSykepengegrunnlag(sykepengegrunnlagId: UUID) {
        db
            .update(
                """
                DELETE FROM sykepengegrunnlag 
                WHERE id = :id and not laast
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to sykepengegrunnlagId,
            ).also(verifiserOppdatert)
    }

    override fun settLåst(sykepengegrunnlagId: UUID) {
        val nå = java.time.Instant.now()

        db
            .update(
                """
                UPDATE sykepengegrunnlag 
                SET laast = true, oppdatert = :oppdatert
                WHERE id = :id and not laast
                """.trimIndent(),
                "id" to sykepengegrunnlagId,
                "oppdatert" to nå,
            ).also(verifiserOppdatert)
    }

    private val verifiserOppdatert: (Int) -> Unit = {
        if (it == 0) {
            throw KunneIkkeOppdatereDbException("Sykepengegrunnlag kunne ikke oppdateres")
        }
    }

    private fun sykepengegrunnlagFraRow(row: Row): SykepengegrunnlagDbRecord {
        val sykepengegrunnlagJson = row.stringOrNull("sykepengegrunnlag")
        val sykepengegrunnlag =
            sykepengegrunnlagJson?.let {
                objectMapper.readValue(sykepengegrunnlagJson, SykepengegrunnlagBase::class.java)
            }

        val sammenlikningsgrunnlagJson = row.stringOrNull("sammenlikningsgrunnlag")
        val sammenlikningsgrunnlag =
            sammenlikningsgrunnlagJson?.let {
                objectMapper.readValue(sammenlikningsgrunnlagJson, Sammenlikningsgrunnlag::class.java)
            }

        val opprettet = row.instant("opprettet")
        val oppdatert = row.instant("oppdatert")

        return SykepengegrunnlagDbRecord(
            sykepengegrunnlag = sykepengegrunnlag,
            id = row.uuid("id"),
            opprettetAv = row.string("opprettet_av_nav_ident"),
            opprettet = opprettet,
            oppdatert = oppdatert,
            sammenlikningsgrunnlag = sammenlikningsgrunnlag,
            opprettetForBehandling = row.uuid("opprettet_for_behandling"),
            låst = row.boolean("laast"),
        )
    }
}
