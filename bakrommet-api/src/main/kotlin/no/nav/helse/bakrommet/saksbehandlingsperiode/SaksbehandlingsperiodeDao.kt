package no.nav.helse.bakrommet.saksbehandlingsperiode

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
data class Saksbehandlingsperiode(
    val id: UUID,
    val spilleromPersonId: String,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: SaksbehandlingsperiodeStatus = SaksbehandlingsperiodeStatus.UNDER_BEHANDLING,
    val beslutterNavIdent: String? = null,
    val skjæringstidspunkt: LocalDate? = null,
    val individuellBegrunnelse: String? = null,
    val sykepengegrunnlagId: UUID? = null,
)

enum class SaksbehandlingsperiodeStatus {
    UNDER_BEHANDLING,
    TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    ;

    companion object {
        val GYLDIGE_ENDRINGER: Set<Pair<SaksbehandlingsperiodeStatus, SaksbehandlingsperiodeStatus>> =
            setOf(
                UNDER_BEHANDLING to TIL_BESLUTNING,
                TIL_BESLUTNING to UNDER_BESLUTNING,
                UNDER_BESLUTNING to GODKJENT,
                UNDER_BESLUTNING to UNDER_BEHANDLING,
                TIL_BESLUTNING to UNDER_BEHANDLING,
                UNDER_BEHANDLING to UNDER_BESLUTNING,
            )

        fun erGyldigEndring(fraTil: Pair<SaksbehandlingsperiodeStatus, SaksbehandlingsperiodeStatus>) = GYLDIGE_ENDRINGER.contains(fraTil)
    }
}

interface SaksbehandlingsperiodeDao {
    suspend fun hentAlleSaksbehandlingsperioder(): List<Saksbehandlingsperiode>

    suspend fun finnSaksbehandlingsperiode(id: UUID): Saksbehandlingsperiode?

    suspend fun finnPerioderForPerson(spilleromPersonId: String): List<Saksbehandlingsperiode>

    suspend fun finnPerioderForPersonSomOverlapper(
        spilleromPersonId: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Saksbehandlingsperiode>

    suspend fun endreStatus(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
    )

    suspend fun endreStatusOgIndividuellBegrunnelse(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
        individuellBegrunnelse: String?,
    )

    suspend fun endreStatusOgBeslutter(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
        beslutterNavIdent: String?,
    )

    suspend fun opprettPeriode(periode: Saksbehandlingsperiode)

    suspend fun oppdaterSkjæringstidspunkt(
        periodeId: UUID,
        skjæringstidspunkt: LocalDate?,
    )

    suspend fun oppdaterSykepengegrunnlagId(
        periodeId: UUID,
        sykepengegrunnlagId: UUID?,
    )
}

class SaksbehandlingsperiodeDaoPg private constructor(
    private val db: QueryRunner,
) : SaksbehandlingsperiodeDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override suspend fun hentAlleSaksbehandlingsperioder(): List<Saksbehandlingsperiode> {
        val limitEnnSåLenge = 100
        return db
            .list(
                """
                select *
                  from saksbehandlingsperiode
                 
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

    override suspend fun finnSaksbehandlingsperiode(id: UUID): Saksbehandlingsperiode? =
        db.single(
            """
            select *
              from saksbehandlingsperiode
             where id = :id
            """.trimIndent(),
            "id" to id,
        ) { rowTilPeriode(it) }

    override suspend fun finnPerioderForPerson(spilleromPersonId: String): List<Saksbehandlingsperiode> =
        db.list(
            """
            select *
              from saksbehandlingsperiode
             where spillerom_personid = :spillerom_personid
            """.trimIndent(),
            "spillerom_personid" to spilleromPersonId,
        ) { rowTilPeriode(it) }

    override suspend fun finnPerioderForPersonSomOverlapper(
        spilleromPersonId: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Saksbehandlingsperiode> =
        db.list(
            """
            select *
              from saksbehandlingsperiode
             where spillerom_personid = :spillerom_personid
             AND fom <= :tom
             AND tom >= :fom
            """.trimIndent(),
            "spillerom_personid" to spilleromPersonId,
            "fom" to fom,
            "tom" to tom,
        ) { rowTilPeriode(it) }

    private fun rowTilPeriode(row: Row) =
        Saksbehandlingsperiode(
            id = row.uuid("id"),
            spilleromPersonId = row.string("spillerom_personid"),
            opprettet = row.offsetDateTime("opprettet"),
            opprettetAvNavIdent = row.string("opprettet_av_nav_ident"),
            opprettetAvNavn = row.string("opprettet_av_navn"),
            fom = row.localDate("fom"),
            tom = row.localDate("tom"),
            status = SaksbehandlingsperiodeStatus.valueOf(row.string("status")),
            beslutterNavIdent = row.stringOrNull("beslutter_nav_ident"),
            skjæringstidspunkt = row.localDateOrNull("skjaeringstidspunkt"),
            individuellBegrunnelse = row.stringOrNull("individuell_begrunnelse"),
            sykepengegrunnlagId = row.uuidOrNull("sykepengegrunnlag_id"),
        )

    override suspend fun endreStatus(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        db.update(
            """
            UPDATE saksbehandlingsperiode SET status = :status
            WHERE id = :id
            """.trimIndent(),
            "id" to periode.id,
            "status" to nyStatus.name,
        )
    }

    override suspend fun endreStatusOgIndividuellBegrunnelse(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
        individuellBegrunnelse: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        db.update(
            """
            UPDATE saksbehandlingsperiode SET status = :status, individuell_begrunnelse = :individuell_begrunnelse
            WHERE id = :id
            """.trimIndent(),
            "id" to periode.id,
            "status" to nyStatus.name,
            "individuell_begrunnelse" to individuellBegrunnelse,
        )
    }

    override suspend fun endreStatusOgBeslutter(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
        beslutterNavIdent: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        db.update(
            """
            UPDATE saksbehandlingsperiode SET status = :status, beslutter_nav_ident = :beslutter_nav_ident
            WHERE id = :id
            """.trimIndent(),
            "id" to periode.id,
            "status" to nyStatus.name,
            "beslutter_nav_ident" to beslutterNavIdent,
        )
    }

    override suspend fun opprettPeriode(periode: Saksbehandlingsperiode) {
        db.update(
            """
            insert into saksbehandlingsperiode
                (id, spillerom_personid, opprettet, opprettet_av_nav_ident, opprettet_av_navn, fom, tom, status, beslutter_nav_ident, skjaeringstidspunkt, individuell_begrunnelse, sykepengegrunnlag_id)
            values
                (:id, :spillerom_personid, :opprettet, :opprettet_av_nav_ident, :opprettet_av_navn, :fom, :tom, :status, :beslutter_nav_ident, :skjaeringstidspunkt, :individuell_begrunnelse, :sykepengegrunnlag_id)
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
        )
    }

    override suspend fun oppdaterSkjæringstidspunkt(
        periodeId: UUID,
        skjæringstidspunkt: LocalDate?,
    ) {
        db.update(
            """
            UPDATE saksbehandlingsperiode 
            SET skjaeringstidspunkt = :skjaeringstidspunkt
            WHERE id = :id
            """.trimIndent(),
            "id" to periodeId,
            "skjaeringstidspunkt" to skjæringstidspunkt,
        )
    }

    override suspend fun oppdaterSykepengegrunnlagId(
        periodeId: UUID,
        sykepengegrunnlagId: UUID?,
    ) {
        db.update(
            """
            UPDATE saksbehandlingsperiode 
            SET sykepengegrunnlag_id = :sykepengegrunnlag_id
            WHERE id = :id
            """.trimIndent(),
            "id" to periodeId,
            "sykepengegrunnlag_id" to sykepengegrunnlagId,
        )
    }
}
