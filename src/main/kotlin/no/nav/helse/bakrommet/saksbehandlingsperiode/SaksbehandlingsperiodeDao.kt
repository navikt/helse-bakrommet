package no.nav.helse.bakrommet.saksbehandlingsperiode

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

class SaksbehandlingsperiodeDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun hentAlleSaksbehandlingsperioder(): List<Saksbehandlingsperiode> {
        val limitEnnSåLenge = 100
        return db.list(
            """
            select *
              from saksbehandlingsperiode
             
              LIMIT $limitEnnSåLenge 
            """.trimIndent(),
        ) { rowTilPeriode(it) }.also { perioderIRetur ->
            if (perioderIRetur.size >= limitEnnSåLenge) {
                // TODO: Det må inn noe WHERE-kriterer og paginering her ...
                appLogger.error("hentSaksbehandlingsperioder out of limit")
            }
        }
    }

    fun finnSaksbehandlingsperiode(id: UUID): Saksbehandlingsperiode? =
        db.single(
            """
            select *
              from saksbehandlingsperiode
             where id = :id
            """.trimIndent(),
            "id" to id,
        ) { rowTilPeriode(it) }

    // Ny metode: finn én periode basert på spillerom_personid
    fun finnPerioderForPerson(spilleromPersonId: String): List<Saksbehandlingsperiode> =
        db.list(
            """
            select *
              from saksbehandlingsperiode
             where spillerom_personid = :spillerom_personid
            """.trimIndent(),
            "spillerom_personid" to spilleromPersonId,
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
        )

    fun endreStatus(
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

    fun endreStatusOgBeslutter(
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

    fun opprettPeriode(periode: Saksbehandlingsperiode) {
        db.update(
            """
            insert into saksbehandlingsperiode
                (id, spillerom_personid, opprettet, opprettet_av_nav_ident, opprettet_av_navn, fom, tom, status, beslutter_nav_ident)
            values
                (:id, :spillerom_personid, :opprettet, :opprettet_av_nav_ident, :opprettet_av_navn, :fom, :tom, :status, :beslutter_nav_ident)
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
        )
    }
}
