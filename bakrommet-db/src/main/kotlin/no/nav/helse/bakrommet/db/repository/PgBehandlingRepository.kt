package no.nav.helse.bakrommet.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringType
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringType.*
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
        ) { rowTilBehandling(it) }

    override fun finnAlle(): List<Behandling> {
        TODO("Ikke implementert i PgBehandlingRepository")
    }

    override fun finnFor(naturligIdent: NaturligIdent): List<Behandling> =
        queryRunner.list(
            """
            select *
              from behandling
             where naturlig_ident = :naturligIdent
            """.trimIndent(),
            "naturligIdent" to naturligIdent.value,
        ) { rowTilBehandling(it) }

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

        val antallEksisterendeEndringer = antallEndringerIDatabasen(behandling.id)
        val nyeEndringer = behandling.endringer.drop(antallEksisterendeEndringer)
        nyeEndringer.forEach { endring ->
            lagre(behandling.id, endring)
        }
    }

    private fun BehandlingId.finnEndringer(): List<Behandling.Endring> =
        queryRunner.list(
            """
            select * from behandling_endringer
                where behandling_id = :behandling_id
                order by endret_tidspunkt
            """.trimIndent(),
            "behandling_id" to this.value,
        ) {
            Behandling.Endring(
                type =
                    when (enumValueOf<SaksbehandlingsperiodeEndringType>(it.string("endring_type"))) {
                        STARTET -> Behandling.Endring.TypeEndring.Startet
                        SENDT_TIL_BESLUTNING -> Behandling.Endring.TypeEndring.SendtTilBeslutning
                        TATT_TIL_BESLUTNING -> Behandling.Endring.TypeEndring.TattTilBeslutning
                        SENDT_I_RETUR -> Behandling.Endring.TypeEndring.SendtIRetur
                        GODKJENT -> Behandling.Endring.TypeEndring.Godkjent
                        OPPDATERT_INDIVIDUELL_BEGRUNNELSE -> Behandling.Endring.TypeEndring.OppdatertIndividuellBegrunnelse
                        OPPDATERT_SKJÆRINGSTIDSPUNKT -> Behandling.Endring.TypeEndring.OppdatertSkjæringstidspunkt
                        OPPDATERT_YRKESAKTIVITET_KATEGORISERING -> Behandling.Endring.TypeEndring.OppdatertYrkesaktivitetKategorisering
                        REVURDERING_STARTET -> Behandling.Endring.TypeEndring.RevurderingStartet
                    },
                tidspunkt = it.instant("endret_tidspunkt"),
                navIdent = it.string("endret_av_nav_ident"),
                status = BehandlingStatus.valueOf(it.string("status")),
                beslutterNavIdent = it.stringOrNull("beslutter_nav_ident"),
                kommentar = it.stringOrNull("endring_kommentar"),
            )
        }

    private fun lagre(
        behandlingId: BehandlingId,
        endring: Behandling.Endring,
    ) {
        queryRunner.update(
            """
            insert into behandling_endringer
                (behandling_id, status, beslutter_nav_ident, endret_tidspunkt, endret_av_nav_ident, endring_type, endring_kommentar)
            values
                (:behandling_id, :status, :beslutter_nav_ident, :endret_tidspunkt, :endret_av_nav_ident, :endring_type, :endring_kommentar)
            """.trimIndent(),
            "behandling_id" to behandlingId.value,
            "status" to
                when (endring.status) {
                    BehandlingStatus.UNDER_BEHANDLING -> "UNDER_BEHANDLING"
                    BehandlingStatus.TIL_BESLUTNING -> "TIL_BESLUTNING"
                    BehandlingStatus.UNDER_BESLUTNING -> "UNDER_BESLUTNING"
                    BehandlingStatus.GODKJENT -> "GODKJENT"
                    BehandlingStatus.REVURDERT -> "REVURDERT"
                },
            "beslutter_nav_ident" to endring.beslutterNavIdent,
            "endret_tidspunkt" to endring.tidspunkt,
            "endret_av_nav_ident" to endring.navIdent,
            "endring_type" to
                when (endring.type) {
                    Behandling.Endring.TypeEndring.Startet -> "STARTET"
                    Behandling.Endring.TypeEndring.SendtTilBeslutning -> "SENDT_TIL_BESLUTNING"
                    Behandling.Endring.TypeEndring.TattTilBeslutning -> "TATT_TIL_BESLUTNING"
                    Behandling.Endring.TypeEndring.SendtIRetur -> "SENDT_I_RETUR"
                    Behandling.Endring.TypeEndring.Godkjent -> "GODKJENT"
                    Behandling.Endring.TypeEndring.OppdatertIndividuellBegrunnelse -> "OPPDATERT_INDIVIDUELL_BEGRUNNELSE"
                    Behandling.Endring.TypeEndring.OppdatertSkjæringstidspunkt -> "OPPDATERT_SKJÆRINGSTIDSPUNKT"
                    Behandling.Endring.TypeEndring.OppdatertYrkesaktivitetKategorisering -> "OPPDATERT_YRKESAKTIVITET_KATEGORISERING"
                    Behandling.Endring.TypeEndring.RevurderingStartet -> "REVURDERING_STARTET"
                },
            "endring_kommentar" to endring.kommentar,
        )
    }

    private fun antallEndringerIDatabasen(behandlingId: BehandlingId): Int =
        queryRunner.single(
            """
            select count(*) as antall
              from behandling_endringer
             where behandling_id = :behandling_id
            """.trimIndent(),
            "behandling_id" to behandlingId.value,
        ) { it.int("antall") } ?: 0

    private fun rowTilBehandling(row: Row): Behandling {
        val behandlingId = BehandlingId(row.uuid("id"))
        return Behandling.fraLagring(
            id = behandlingId,
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
            endringer = behandlingId.finnEndringer(),
        )
    }
}
