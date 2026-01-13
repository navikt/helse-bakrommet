package no.nav.helse.bakrommet.db.dao

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.appLogger
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

private val verifiserOppdatert: (Int) -> Unit = {
    if (it == 0) {
        throw KunneIkkeOppdatereDbException("Behandling kunne ikke oppdateres")
    }
}

internal const val STATUS_UNDER_BEHANDLING_STR: String = "UNDER_BEHANDLING"

private const val AND_ER_UNDER_BEHANDLING = "AND status  = '${STATUS_UNDER_BEHANDLING_STR}'"

class BehandlingDaoPg private constructor(
    private val db: QueryRunner,
) : BehandlingDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun hentAlleBehandlinger(): List<BehandlingDbRecord> {
        val limitEnnSåLenge = 100
        return db
            .list(
                """
                select *
                  from behandling
                 
                  LIMIT $limitEnnSåLenge 
                """.trimIndent(),
            ) { rowTilPeriode(it) }
            .also { perioderIRetur ->
                if (perioderIRetur.size >= limitEnnSåLenge) {
                    // TODO: Det må inn noe WHERE-kriterer og paginering her ...
                    appLogger.error("hentSaksbehandlingsperioder out of limit")
                }
            }
    }

    override fun finnBehandling(id: UUID): BehandlingDbRecord? =
        db.single(
            """
            select *
              from behandling
             where id = :id
            """.trimIndent(),
            "id" to id,
        ) { rowTilPeriode(it) }

    override fun finnBehandlingerForNaturligIdent(naturligIdent: NaturligIdent): List<BehandlingDbRecord> =
        db.list(
            """
            select *
              from behandling
             where naturlig_ident = :naturlig_ident
            """.trimIndent(),
            "naturlig_ident" to naturligIdent.value,
        ) { rowTilPeriode(it) }

    override fun finnBehandlingerForNaturligIdentSomOverlapper(
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        tom: LocalDate,
    ): List<BehandlingDbRecord> =
        db.list(
            """
            select *
              from behandling
             where naturlig_ident = :naturlig_ident
             AND fom <= :tom
             AND tom >= :fom
            """.trimIndent(),
            "naturlig_ident" to naturligIdent.value,
            "fom" to fom,
            "tom" to tom,
        ) { rowTilPeriode(it) }

    private fun rowTilPeriode(row: Row) =
        BehandlingDbRecord(
            id = row.uuid("id"),
            naturligIdent = NaturligIdent(row.string("naturlig_ident")),
            opprettet = row.offsetDateTime("opprettet"),
            opprettetAvNavIdent = row.string("opprettet_av_nav_ident"),
            opprettetAvNavn = row.string("opprettet_av_navn"),
            fom = row.localDate("fom"),
            tom = row.localDate("tom"),
            status = BehandlingStatus.valueOf(row.string("status")),
            beslutterNavIdent = row.stringOrNull("beslutter_nav_ident"),
            skjæringstidspunkt = row.localDate("skjaeringstidspunkt"),
            individuellBegrunnelse = row.stringOrNull("individuell_begrunnelse"),
            sykepengegrunnlagId = row.uuidOrNull("sykepengegrunnlag_id"),
            revurdererSaksbehandlingsperiodeId = row.uuidOrNull("revurderer_behandling_id"),
            revurdertAvBehandlingId = row.uuidOrNull("revurdert_av_behandling_id"),
        )

    override fun endreStatus(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        db.update(
            """
            UPDATE behandling SET status = :status
            WHERE id = :id
            """.trimIndent(),
            "id" to periode.id,
            "status" to nyStatus.name,
        )
    }

    override fun endreStatusOgIndividuellBegrunnelse(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
        individuellBegrunnelse: String?,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        db.update(
            """
            UPDATE behandling SET status = :status, individuell_begrunnelse = :individuell_begrunnelse
            WHERE id = :id
            """.trimIndent(),
            "id" to periode.id,
            "status" to nyStatus.name,
            "individuell_begrunnelse" to individuellBegrunnelse,
        )
    }

    override fun endreStatusOgBeslutter(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
        beslutterNavIdent: String?,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        db.update(
            """
            UPDATE behandling SET status = :status, beslutter_nav_ident = :beslutter_nav_ident
            WHERE id = :id
            """.trimIndent(),
            "id" to periode.id,
            "status" to nyStatus.name,
            "beslutter_nav_ident" to beslutterNavIdent,
        )
    }

    override fun opprettPeriode(periode: BehandlingDbRecord) {
        db.update(
            """
            insert into behandling
                (id, naturlig_ident, opprettet, opprettet_av_nav_ident, opprettet_av_navn, fom, tom, status, beslutter_nav_ident, skjaeringstidspunkt, individuell_begrunnelse, sykepengegrunnlag_id, revurderer_behandling_id)
            values
                (:id, :naturlig_ident, :opprettet, :opprettet_av_nav_ident, :opprettet_av_navn, :fom, :tom, :status, :beslutter_nav_ident, :skjaeringstidspunkt, :individuell_begrunnelse, :sykepengegrunnlag_id, :revurderer_behandling_id)
            """.trimIndent(),
            "id" to periode.id,
            "naturlig_ident" to periode.naturligIdent.value,
            "opprettet" to periode.opprettet,
            "opprettet_av_nav_ident" to periode.opprettetAvNavIdent,
            "opprettet_av_navn" to periode.opprettetAvNavn,
            "fom" to periode.fom,
            "tom" to periode.tom,
            "status" to periode.status.name,
            "beslutter_nav_ident" to periode.beslutterNavIdent,
            "skjaeringstidspunkt" to periode.skjæringstidspunkt,
            "individuell_begrunnelse" to periode.individuellBegrunnelse,
            "sykepengegrunnlag_id" to periode.sykepengegrunnlagId,
            "revurderer_behandling_id" to periode.revurdererSaksbehandlingsperiodeId,
        )
    }

    override fun oppdaterSkjæringstidspunkt(
        behandlingId: UUID,
        skjæringstidspunkt: LocalDate,
    ) {
        db
            .update(
                """
                UPDATE behandling 
                SET skjaeringstidspunkt = :skjaeringstidspunkt
                WHERE id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to behandlingId,
                "skjaeringstidspunkt" to skjæringstidspunkt,
            ).also(verifiserOppdatert)
    }

    override fun oppdaterSykepengegrunnlagId(
        behandlingId: UUID,
        sykepengegrunnlagId: UUID?,
    ) {
        db
            .update(
                """
                UPDATE behandling 
                SET sykepengegrunnlag_id = :sykepengegrunnlag_id
                WHERE id = :id
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "id" to behandlingId,
                "sykepengegrunnlag_id" to sykepengegrunnlagId,
            ).also(verifiserOppdatert)
    }

    override fun oppdaterRevurdertAvBehandlingId(
        behandlingId: UUID,
        revurdertAvBehandlingId: UUID,
    ) {
        db
            .update(
                """
                UPDATE behandling 
                SET revurdert_av_behandling_id = :revurdert_av_behandling_id
                WHERE id = :id
                """.trimIndent() +
                    /*
                     TODO: Det blir vel riktig dette ?
                     status settes til REVURDERT rett før (i en transaksjon).
                     Skal ikke kunne være tidligere revurdert, da må man vel heller revurdere revurderingen ?
                     */
                    """
            AND status = 'REVURDERT'
            AND revurdert_av_behandling_id IS NULL
            """,
                "id" to behandlingId,
                "revurdert_av_behandling_id" to revurdertAvBehandlingId,
            ).also(verifiserOppdatert)
    }
}
