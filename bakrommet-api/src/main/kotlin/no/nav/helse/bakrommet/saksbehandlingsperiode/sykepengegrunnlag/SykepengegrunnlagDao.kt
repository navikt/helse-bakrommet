package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import java.util.UUID
import javax.sql.DataSource

interface SykepengegrunnlagDao {
    fun lagreSykepengegrunnlag(
        sykepengegrunnlag: Sykepengegrunnlag?,
        saksbehandler: Bruker,
        opprettetForBehandling: UUID,
    ): SykepengegrunnlagDbRecord

    fun finnSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord?

    fun hentSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord =
        finnSykepengegrunnlag(sykepengegrunnlagId)
            ?: throw IllegalArgumentException("Fant ikke sykepengegrunnlag for id $sykepengegrunnlagId")

    fun oppdaterSykepengegrunnlag(
        sykepengegrunnlagId: UUID,
        sykepengegrunnlag: Sykepengegrunnlag?,
    ): SykepengegrunnlagDbRecord

    fun oppdaterSammenlikningsgrunnlag(
        sykepengegrunnlagId: UUID,
        sammenlikningsgrunnlag: Sammenlikningsgrunnlag?,
    ): SykepengegrunnlagDbRecord

    fun slettSykepengegrunnlag(sykepengegrunnlagId: UUID)
}

class SykepengegrunnlagDaoPg private constructor(
    private val db: QueryRunner,
) : SykepengegrunnlagDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun lagreSykepengegrunnlag(
        sykepengegrunnlag: Sykepengegrunnlag?,
        saksbehandler: Bruker,
        opprettetForBehandling: UUID,
    ): SykepengegrunnlagDbRecord {
        val id = UUID.randomUUID()
        val nå = java.time.Instant.now()
        val sykepengegrunnlagJson = objectMapperCustomSerde.writeValueAsString(sykepengegrunnlag)

        db.update(
            """
            INSERT INTO sykepengegrunnlag (id, sykepengegrunnlag, opprettet_av_nav_ident, opprettet, oppdatert, opprettet_for_behandling)
            VALUES (:id, :sykepengegrunnlag, :opprettet_av_nav_ident, :opprettet, :oppdatert, :opprettet_for_behandling)
            """.trimIndent(),
            "id" to id,
            "sykepengegrunnlag" to sykepengegrunnlagJson,
            "opprettet_av_nav_ident" to saksbehandler.navIdent,
            "opprettet" to nå,
            "oppdatert" to nå,
            "opprettet_for_behandling" to opprettetForBehandling,
        )

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
        sykepengegrunnlag: Sykepengegrunnlag?,
    ): SykepengegrunnlagDbRecord {
        val nå = java.time.Instant.now()
        val sykepengegrunnlagJson = sykepengegrunnlag?.let { objectMapperCustomSerde.writeValueAsString(it) }

        db.update(
            """
            UPDATE sykepengegrunnlag 
            SET sykepengegrunnlag = :sykepengegrunnlag, oppdatert = :oppdatert
            WHERE id = :id
            """.trimIndent(),
            "id" to sykepengegrunnlagId,
            "sykepengegrunnlag" to sykepengegrunnlagJson,
            "oppdatert" to nå,
        )

        return finnSykepengegrunnlag(sykepengegrunnlagId)!!
    }

    override fun oppdaterSammenlikningsgrunnlag(
        sykepengegrunnlagId: UUID,
        sammenlikningsgrunnlag: Sammenlikningsgrunnlag?,
    ): SykepengegrunnlagDbRecord {
        require(finnSykepengegrunnlag(sykepengegrunnlagId)!!.sammenlikningsgrunnlag == null)

        val nå = java.time.Instant.now()
        val sammenlikningsgrunnlagJson = sammenlikningsgrunnlag?.let { objectMapperCustomSerde.writeValueAsString(it) }

        db.update(
            """
            UPDATE sykepengegrunnlag 
            SET sammenlikningsgrunnlag = :sammenlikningsgrunnlag, oppdatert = :oppdatert
            WHERE id = :id
            """.trimIndent(),
            "id" to sykepengegrunnlagId,
            "sammenlikningsgrunnlag" to sammenlikningsgrunnlagJson,
            "oppdatert" to nå,
        )

        return finnSykepengegrunnlag(sykepengegrunnlagId)!!
    }

    override fun slettSykepengegrunnlag(sykepengegrunnlagId: UUID) {
        db.update(
            """
            DELETE FROM sykepengegrunnlag 
            WHERE id = :id
            """.trimIndent(),
            "id" to sykepengegrunnlagId,
        )
    }

    private fun sykepengegrunnlagFraRow(row: Row): SykepengegrunnlagDbRecord {
        val sykepengegrunnlagJson = row.stringOrNull("sykepengegrunnlag")
        val sykepengegrunnlag =
            sykepengegrunnlagJson?.let {
                objectMapperCustomSerde.readValue(sykepengegrunnlagJson, Sykepengegrunnlag::class.java)
            }

        val sammenlikningsgrunnlagJson = row.stringOrNull("sammenlikningsgrunnlag")
        val sammenlikningsgrunnlag =
            sammenlikningsgrunnlagJson?.let {
                objectMapperCustomSerde.readValue(sammenlikningsgrunnlagJson, Sammenlikningsgrunnlag::class.java)
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
        )
    }
}
