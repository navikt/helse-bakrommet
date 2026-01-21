package no.nav.helse.bakrommet.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus
import no.nav.helse.bakrommet.repository.BehandlingRepository

class PgBehandlingRepository private constructor(
    private val queryRunner: QueryRunner,
) : BehandlingRepository {
    constructor(session: Session) : this(MedSession(session))

    override fun finn(behandlingId: BehandlingId): Behandling? =
        queryRunner.single(
            """
            select *
              from behandling
             where id = :id
            """.trimIndent(),
            "id" to behandlingId.value,
        ) { rowTilPeriode(it) }

    override fun finnAlle(): List<Behandling> {
        TODO("Ikke implementert i PgBehandlingRepository")
    }

    override fun finnFor(naturligIdent: NaturligIdent): List<Behandling> {
        TODO("Ikke implementert i PgBehandlingRepository")
    }

    override fun lagre(behandling: Behandling) {
        queryRunner.update(
            """
            insert into behandling
                (id, naturlig_ident, opprettet, opprettet_av_nav_ident, opprettet_av_navn, fom, tom, status, beslutter_nav_ident, skjaeringstidspunkt, individuell_begrunnelse, sykepengegrunnlag_id, revurderer_behandling_id, revurdert_av_behandling_id)
            values
                (:id, :naturlig_ident, :opprettet, :opprettet_av_nav_ident, :opprettet_av_navn, :fom, :tom, :status, :beslutter_nav_ident, :skjaeringstidspunkt, :individuell_begrunnelse, :sykepengegrunnlag_id, :revurderer_behandling_id, :revurdert_av_behandling_id)
            on conflict(id) do update SET
                fom = excluded.fom,
                tom = excluded.tom,
                status = excluded.status,
                beslutter_nav_ident = excluded.beslutter_nav_ident,
                skjaeringstidspunkt = excluded.skjaeringstidspunkt,
                individuell_begrunnelse = excluded.individuell_begrunnelse,
                sykepengegrunnlag_id = excluded.sykepengegrunnlag_id,
                revurderer_behandling_id = excluded.revurderer_behandling_id,
                revurdert_av_behandling_id = excluded.revurdert_av_behandling_id
            """.trimIndent(),
            "id" to behandling.id.value,
            "naturlig_ident" to behandling.naturligIdent.value,
            "opprettet" to behandling.opprettet,
            "opprettet_av_nav_ident" to behandling.opprettetAvNavIdent,
            "opprettet_av_navn" to behandling.opprettetAvNavn,
            "fom" to behandling.fom,
            "tom" to behandling.tom,
            "status" to behandling.status.name,
            "beslutter_nav_ident" to behandling.beslutterNavIdent,
            "skjaeringstidspunkt" to behandling.skjæringstidspunkt,
            "individuell_begrunnelse" to behandling.individuellBegrunnelse,
            "sykepengegrunnlag_id" to behandling.sykepengegrunnlagId,
            "revurderer_behandling_id" to behandling.revurdererBehandlingId?.value,
            "revurdert_av_behandling_id" to behandling.revurdertAvBehandlingId?.value,
        )
    }

    private fun rowTilPeriode(row: Row) =
        Behandling.fraLagring(
            id = BehandlingId(row.uuid("id")),
            naturligIdent = NaturligIdent(row.string("naturlig_ident")),
            opprettet = row.instant("opprettet"),
            opprettetAvNavIdent = row.string("opprettet_av_nav_ident"),
            opprettetAvNavn = row.string("opprettet_av_navn"),
            fom = row.localDate("fom"),
            tom = row.localDate("tom"),
            status = BehandlingStatus.valueOf(row.string("status")),
            beslutterNavIdent = row.stringOrNull("beslutter_nav_ident"),
            skjæringstidspunkt = row.localDate("skjaeringstidspunkt"),
            individuellBegrunnelse = row.stringOrNull("individuell_begrunnelse"),
            sykepengegrunnlagId = row.uuidOrNull("sykepengegrunnlag_id"),
            revurdererBehandlingId = row.uuidOrNull("revurderer_behandling_id")?.let { BehandlingId(it) },
            revurdertAvBehandlingId = row.uuidOrNull("revurdert_av_behandling_id")?.let { BehandlingId(it) },
        )
}
