package no.nav.helse.bakrommet.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.appLogger
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.person.NaturligIdent
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Behandling(
    val id: UUID,
    val naturligIdent: NaturligIdent,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: BehandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
    val beslutterNavIdent: String? = null,
    val skjæringstidspunkt: LocalDate,
    val individuellBegrunnelse: String? = null,
    val sykepengegrunnlagId: UUID? = null,
    val revurdererSaksbehandlingsperiodeId: UUID? = null,
    val revurdertAvBehandlingId: UUID? = null,
)

enum class BehandlingStatus {
    UNDER_BEHANDLING,
    TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    REVURDERT,
    ;

    companion object {
        val GYLDIGE_ENDRINGER: Set<Pair<BehandlingStatus, BehandlingStatus>> =
            setOf(
                UNDER_BEHANDLING to TIL_BESLUTNING,
                TIL_BESLUTNING to UNDER_BESLUTNING,
                UNDER_BESLUTNING to GODKJENT,
                UNDER_BESLUTNING to UNDER_BEHANDLING,
                UNDER_BESLUTNING to UNDER_BESLUTNING, // ved endring av beslutter
                TIL_BESLUTNING to UNDER_BEHANDLING,
                UNDER_BEHANDLING to UNDER_BESLUTNING,
                GODKJENT to REVURDERT,
            )

        fun erGyldigEndring(fraTil: Pair<BehandlingStatus, BehandlingStatus>) = GYLDIGE_ENDRINGER.contains(fraTil)
    }
}

interface BehandlingDao {
    fun hentAlleBehandlinger(): List<Behandling>

    fun finnBehandling(id: UUID): Behandling?

    fun finnBehandlingerForNaturligIdent(naturligIdent: NaturligIdent): List<Behandling>

    fun finnBehandlingerForNaturligIdentSomOverlapper(
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Behandling>

    fun endreStatus(
        periode: Behandling,
        nyStatus: BehandlingStatus,
    )

    fun endreStatusOgIndividuellBegrunnelse(
        periode: Behandling,
        nyStatus: BehandlingStatus,
        individuellBegrunnelse: String?,
    )

    fun endreStatusOgBeslutter(
        periode: Behandling,
        nyStatus: BehandlingStatus,
        beslutterNavIdent: String?,
    )

    fun opprettPeriode(periode: Behandling)

    fun oppdaterSkjæringstidspunkt(
        periodeId: UUID,
        skjæringstidspunkt: LocalDate,
    )

    fun oppdaterSykepengegrunnlagId(
        periodeId: UUID,
        sykepengegrunnlagId: UUID?,
    )

    fun oppdaterRevurdertAvBehandlingId(
        behandlingId: UUID,
        revurdertAvBehandlingId: UUID,
    )
}

private val verifiserOppdatert: (Int) -> Unit = {
    if (it == 0) {
        throw KunneIkkeOppdatereDbException("Behandling kunne ikke oppdateres")
    }
}

private const val AND_ER_UNDER_BEHANDLING = "AND status  = '$STATUS_UNDER_BEHANDLING_STR'"

class BehandlingDaoPg private constructor(
    private val db: QueryRunner,
) : BehandlingDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun hentAlleBehandlinger(): List<Behandling> {
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

    override fun finnBehandling(id: UUID): Behandling? =
        db.single(
            """
            select *
              from behandling
             where id = :id
            """.trimIndent(),
            "id" to id,
        ) { rowTilPeriode(it) }

    override fun finnBehandlingerForNaturligIdent(naturligIdent: NaturligIdent): List<Behandling> =
        db.list(
            """
            select *
              from behandling
             where naturlig_ident = :naturlig_ident
            """.trimIndent(),
            "naturlig_ident" to naturligIdent.naturligIdent,
        ) { rowTilPeriode(it) }

    override fun finnBehandlingerForNaturligIdentSomOverlapper(
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Behandling> =
        db.list(
            """
            select *
              from behandling
             where naturlig_ident = :naturlig_ident
             AND fom <= :tom
             AND tom >= :fom
            """.trimIndent(),
            "naturlig_ident" to naturligIdent.naturligIdent,
            "fom" to fom,
            "tom" to tom,
        ) { rowTilPeriode(it) }

    private fun rowTilPeriode(row: Row) =
        Behandling(
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
        periode: Behandling,
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
        periode: Behandling,
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
        periode: Behandling,
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

    override fun opprettPeriode(periode: Behandling) {
        db.update(
            """
            insert into behandling
                (id, naturlig_ident, opprettet, opprettet_av_nav_ident, opprettet_av_navn, fom, tom, status, beslutter_nav_ident, skjaeringstidspunkt, individuell_begrunnelse, sykepengegrunnlag_id, revurderer_behandling_id)
            values
                (:id, :naturlig_ident, :opprettet, :opprettet_av_nav_ident, :opprettet_av_navn, :fom, :tom, :status, :beslutter_nav_ident, :skjaeringstidspunkt, :individuell_begrunnelse, :sykepengegrunnlag_id, :revurderer_behandling_id)
            """.trimIndent(),
            "id" to periode.id,
            "naturlig_ident" to periode.naturligIdent,
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
        periodeId: UUID,
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
                "id" to periodeId,
                "skjaeringstidspunkt" to skjæringstidspunkt,
            ).also(verifiserOppdatert)
    }

    override fun oppdaterSykepengegrunnlagId(
        periodeId: UUID,
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
                "id" to periodeId,
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
