package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.util.objectMapper
import java.util.UUID
import javax.sql.DataSource

class SykepengegrunnlagDao private constructor(
    private val db: QueryRunner,
) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun lagreSykepengegrunnlag(
        sykepengegrunnlag: Sykepengegrunnlag,
        saksbehandler: Bruker,
    ): SykepengegrunnlagDbRecord {
        val id = UUID.randomUUID()
        val nå = java.time.Instant.now()
        val sykepengegrunnlagJson = objectMapper.writeValueAsString(sykepengegrunnlag)

        db.update(
            """
            INSERT INTO sykepengegrunnlag (id, sykepengegrunnlag, opprettet_av_nav_ident, opprettet, oppdatert)
            VALUES (:id, :sykepengegrunnlag, :opprettet_av_nav_ident, :opprettet, :oppdatert)
            """.trimIndent(),
            "id" to id,
            "sykepengegrunnlag" to sykepengegrunnlagJson,
            "opprettet_av_nav_ident" to saksbehandler.navIdent,
            "opprettet" to nå,
            "oppdatert" to nå,
        )

        return SykepengegrunnlagDbRecord(
            sykepengegrunnlag = sykepengegrunnlag,
            id = id,
            opprettetAv = saksbehandler.navIdent,
            opprettet = nå,
            oppdatert = nå,
        )
    }

    fun hentSykepengegrunnlag(sykepengrgrunnlagId: UUID): SykepengegrunnlagDbRecord? =
        db
            .list(
                """
                SELECT id, sykepengegrunnlag, opprettet_av_nav_ident, opprettet, oppdatert
                FROM sykepengegrunnlag
                WHERE id = :id
                """.trimIndent(),
                "id" to sykepengrgrunnlagId,
                mapper = ::sykepengegrunnlagFraRow,
            ).firstOrNull()

    fun oppdaterSykepengrgrunnlag(
        saksbehandlingsperiodeId: UUID,
        sykepengegrunnlag: Sykepengegrunnlag,
    ): SykepengegrunnlagDbRecord {
        val nå = java.time.Instant.now()
        val sykepengegrunnlagJson = objectMapper.writeValueAsString(sykepengegrunnlag)

        db.update(
            """
            UPDATE sykepengegrunnlag 
            SET sykepengegrunnlag = :sykepengegrunnlag, oppdatert = :oppdatert
            WHERE id = :id
            """.trimIndent(),
            "id" to saksbehandlingsperiodeId,
            "sykepengegrunnlag" to sykepengegrunnlagJson,
            "oppdatert" to nå,
        )

        return hentSykepengegrunnlag(saksbehandlingsperiodeId)!!
    }

    fun slettSykepengegrunnlag(sykepengegrunnlagId: UUID) {
        db.update(
            """
            DELETE FROM sykepengegrunnlag 
            WHERE id = :id
            """.trimIndent(),
            "id" to sykepengegrunnlagId,
        )
    }

    private fun sykepengegrunnlagFraRow(row: Row): SykepengegrunnlagDbRecord {
        val sykepengegrunnlagJson = row.string("sykepengegrunnlag")
        val sykepengegrunnlag = objectMapper.readValue(sykepengegrunnlagJson, Sykepengegrunnlag::class.java)
        val opprettet = row.instant("opprettet")
        val oppdatert = row.instant("oppdatert")

        return SykepengegrunnlagDbRecord(
            sykepengegrunnlag = sykepengegrunnlag,
            id = row.uuid("id"),
            opprettetAv = row.string("opprettet_av_nav_ident"),
            opprettet = opprettet,
            oppdatert = oppdatert,
        )
    }
}
