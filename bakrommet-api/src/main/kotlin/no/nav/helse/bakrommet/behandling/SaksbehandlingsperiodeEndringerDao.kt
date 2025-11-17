package no.nav.helse.bakrommet.behandling

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

enum class SaksbehandlingsperiodeEndringType {
    STARTET,
    SENDT_TIL_BESLUTNING,
    TATT_TIL_BESLUTNING,
    SENDT_I_RETUR,
    GODKJENT,
    OPPDATERT_INDIVIDUELL_BEGRUNNELSE,
    OPPDATERT_SKJÆRINGSTIDSPUNKT,
    OPPDATERT_YRKESAKTIVITET_KATEGORISERING,
    REVURDERING_STARTET,
}

data class SaksbehandlingsperiodeEndring(
    val saksbehandlingsperiodeId: UUID,
    // //
    val status: SaksbehandlingsperiodeStatus,
    val beslutterNavIdent: String?,
    // //
    val endretTidspunkt: OffsetDateTime,
    val endretAvNavIdent: String,
    val endringType: SaksbehandlingsperiodeEndringType,
    val endringKommentar: String? = null,
)

interface SaksbehandlingsperiodeEndringerDao {
    fun leggTilEndring(hist: SaksbehandlingsperiodeEndring)

    fun hentEndringerFor(saksbehandlingsperiodeId: UUID): List<SaksbehandlingsperiodeEndring>
}

class SaksbehandlingsperiodeEndringerDaoPg private constructor(
    private val db: QueryRunner,
) : SaksbehandlingsperiodeEndringerDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun leggTilEndring(hist: SaksbehandlingsperiodeEndring) {
        db.update(
            """
            insert into behandling_endringer
                (behandling_id, status, beslutter_nav_ident, endret_tidspunkt, endret_av_nav_ident, endring_type, endring_kommentar)
            values
                (:behandling_id, :status, :beslutter_nav_ident, :endret_tidspunkt, :endret_av_nav_ident, :endring_type, :endring_kommentar)
            """.trimIndent(),
            "behandling_id" to hist.saksbehandlingsperiodeId,
            "status" to hist.status.name,
            "beslutter_nav_ident" to hist.beslutterNavIdent,
            "endret_tidspunkt" to hist.endretTidspunkt,
            "endret_av_nav_ident" to hist.endretAvNavIdent,
            "endring_type" to hist.endringType.name,
            "endring_kommentar" to hist.endringKommentar,
        )
    }

    override fun hentEndringerFor(saksbehandlingsperiodeId: UUID): List<SaksbehandlingsperiodeEndring> =
        db.list(
            """
            select *
              from behandling_endringer
              where behandling_id = :behandling_id
              order by id
            """.trimIndent(),
            "behandling_id" to saksbehandlingsperiodeId,
        ) { rowTilHistorikk(it) }

    private fun rowTilHistorikk(row: Row) =
        SaksbehandlingsperiodeEndring(
            saksbehandlingsperiodeId = row.uuid("behandling_id"),
            status = SaksbehandlingsperiodeStatus.valueOf(row.string("status")),
            beslutterNavIdent = row.stringOrNull("beslutter_nav_ident"),
            endretTidspunkt = row.offsetDateTime("endret_tidspunkt"),
            endretAvNavIdent = row.string("endret_av_nav_ident"),
            endringType = SaksbehandlingsperiodeEndringType.valueOf(row.string("endring_type")),
            endringKommentar = row.stringOrNull("endring_kommentar"),
        )
}
