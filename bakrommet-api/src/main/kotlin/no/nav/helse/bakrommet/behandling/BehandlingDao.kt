package no.nav.helse.bakrommet.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.appLogger
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Behandling(
    val id: UUID,
    val spilleromPersonId: String,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: SaksbehandlingsperiodeStatus = SaksbehandlingsperiodeStatus.UNDER_BEHANDLING,
    val beslutterNavIdent: String? = null,
    val skjæringstidspunkt: LocalDate,
    val individuellBegrunnelse: String? = null,
    val sykepengegrunnlagId: UUID? = null,
    val revurdererSaksbehandlingsperiodeId: UUID? = null,
    val revurdertAvBehandlingId: UUID? = null,
)

enum class SaksbehandlingsperiodeStatus {
    UNDER_BEHANDLING,
    TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    REVURDERT,
    ;

    companion object {
        val GYLDIGE_ENDRINGER: Set<Pair<SaksbehandlingsperiodeStatus, SaksbehandlingsperiodeStatus>> =
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

        fun erGyldigEndring(fraTil: Pair<SaksbehandlingsperiodeStatus, SaksbehandlingsperiodeStatus>) = GYLDIGE_ENDRINGER.contains(fraTil)
    }
}

interface BehandlingDao {
    fun hentAlleBehandlinger(): List<Behandling>

    fun finnBehandling(id: UUID): Behandling?

    fun finnBehandlingerForPerson(spilleromPersonId: String): List<Behandling>

    fun finnBehandlingerForPersonSomOverlapper(
        spilleromPersonId: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Behandling>

    fun endreStatus(
        periode: Behandling,
        nyStatus: SaksbehandlingsperiodeStatus,
    )

    fun endreStatusOgIndividuellBegrunnelse(
        periode: Behandling,
        nyStatus: SaksbehandlingsperiodeStatus,
        individuellBegrunnelse: String?,
    )

    fun endreStatusOgBeslutter(
        periode: Behandling,
        nyStatus: SaksbehandlingsperiodeStatus,
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

    override fun finnBehandlingerForPerson(spilleromPersonId: String): List<Behandling> =
        db.list(
            """
            select *
              from behandling
             where spillerom_personid = :spillerom_personid
            """.trimIndent(),
            "spillerom_personid" to spilleromPersonId,
        ) { rowTilPeriode(it) }

    override fun finnBehandlingerForPersonSomOverlapper(
        spilleromPersonId: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Behandling> =
        db.list(
            """
            select *
              from behandling
             where spillerom_personid = :spillerom_personid
             AND fom <= :tom
             AND tom >= :fom
            """.trimIndent(),
            "spillerom_personid" to spilleromPersonId,
            "fom" to fom,
            "tom" to tom,
        ) { rowTilPeriode(it) }

    private fun rowTilPeriode(row: Row) =
        Behandling(
            id = row.uuid("id"),
            spilleromPersonId = row.string("spillerom_personid"),
            opprettet = row.offsetDateTime("opprettet"),
            opprettetAvNavIdent = row.string("opprettet_av_nav_ident"),
            opprettetAvNavn = row.string("opprettet_av_navn"),
            fom = row.localDate("fom"),
            tom = row.localDate("tom"),
            status = SaksbehandlingsperiodeStatus.valueOf(row.string("status")),
            beslutterNavIdent = row.stringOrNull("beslutter_nav_ident"),
            skjæringstidspunkt = row.localDate("skjaeringstidspunkt"),
            individuellBegrunnelse = row.stringOrNull("individuell_begrunnelse"),
            sykepengegrunnlagId = row.uuidOrNull("sykepengegrunnlag_id"),
            revurdererSaksbehandlingsperiodeId = row.uuidOrNull("revurderer_behandling_id"),
            revurdertAvBehandlingId = row.uuidOrNull("revurdert_av_behandling_id"),
        )

    override fun endreStatus(
        periode: Behandling,
        nyStatus: SaksbehandlingsperiodeStatus,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
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
        nyStatus: SaksbehandlingsperiodeStatus,
        individuellBegrunnelse: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
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
        nyStatus: SaksbehandlingsperiodeStatus,
        beslutterNavIdent: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
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
                (id, spillerom_personid, opprettet, opprettet_av_nav_ident, opprettet_av_navn, fom, tom, status, beslutter_nav_ident, skjaeringstidspunkt, individuell_begrunnelse, sykepengegrunnlag_id, revurderer_behandling_id)
            values
                (:id, :spillerom_personid, :opprettet, :opprettet_av_nav_ident, :opprettet_av_navn, :fom, :tom, :status, :beslutter_nav_ident, :skjaeringstidspunkt, :individuell_begrunnelse, :sykepengegrunnlag_id, :revurderer_behandling_id)
            """.trimIndent(),
            "id" to periode.id,
            "spillerom_personid" to periode.spilleromPersonId,
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
        db.update(
            """
            UPDATE behandling 
            SET skjaeringstidspunkt = :skjaeringstidspunkt
            WHERE id = :id
            """.trimIndent(),
            "id" to periodeId,
            "skjaeringstidspunkt" to skjæringstidspunkt,
        )
    }

    override fun oppdaterSykepengegrunnlagId(
        periodeId: UUID,
        sykepengegrunnlagId: UUID?,
    ) {
        db.update(
            """
            UPDATE behandling 
            SET sykepengegrunnlag_id = :sykepengegrunnlag_id
            WHERE id = :id
            """.trimIndent(),
            "id" to periodeId,
            "sykepengegrunnlag_id" to sykepengegrunnlagId,
        )
    }

    override fun oppdaterRevurdertAvBehandlingId(
        behandlingId: UUID,
        revurdertAvBehandlingId: UUID,
    ) {
        db.update(
            """
            UPDATE behandling 
            SET revurdert_av_behandling_id = :revurdert_av_behandling_id
            WHERE id = :id
            """.trimIndent(),
            "id" to behandlingId,
            "revurdert_av_behandling_id" to revurdertAvBehandlingId,
        )
    }
}
