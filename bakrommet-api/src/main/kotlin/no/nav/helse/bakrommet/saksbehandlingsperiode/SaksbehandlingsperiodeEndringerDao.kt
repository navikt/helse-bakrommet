package no.nav.helse.bakrommet.saksbehandlingsperiode

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
    suspend fun leggTilEndring(hist: SaksbehandlingsperiodeEndring)

    suspend fun hentEndringerFor(saksbehandlingsperiodeId: UUID): List<SaksbehandlingsperiodeEndring>
}

class SaksbehandlingsperiodeEndringerDaoPg private constructor(
    private val db: QueryRunner,
) : SaksbehandlingsperiodeEndringerDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override suspend fun leggTilEndring(hist: SaksbehandlingsperiodeEndring) {
        db.update(
            """
            insert into saksbehandlingsperiode_endringer
                (saksbehandlingsperiode_id, status, beslutter_nav_ident, endret_tidspunkt, endret_av_nav_ident, endring_type, endring_kommentar)
            values
                (:saksbehandlingsperiode_id, :status, :beslutter_nav_ident, :endret_tidspunkt, :endret_av_nav_ident, :endring_type, :endring_kommentar)
            """.trimIndent(),
            "saksbehandlingsperiode_id" to hist.saksbehandlingsperiodeId,
            "status" to hist.status.name,
            "beslutter_nav_ident" to hist.beslutterNavIdent,
            "endret_tidspunkt" to hist.endretTidspunkt,
            "endret_av_nav_ident" to hist.endretAvNavIdent,
            "endring_type" to hist.endringType.name,
            "endring_kommentar" to hist.endringKommentar,
        )
    }

    override suspend fun hentEndringerFor(saksbehandlingsperiodeId: UUID): List<SaksbehandlingsperiodeEndring> =
        db.list(
            """
            select *
              from saksbehandlingsperiode_endringer
              where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
              order by id
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
        ) { rowTilHistorikk(it) }

    private fun rowTilHistorikk(row: Row) =
        SaksbehandlingsperiodeEndring(
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            status = SaksbehandlingsperiodeStatus.valueOf(row.string("status")),
            beslutterNavIdent = row.stringOrNull("beslutter_nav_ident"),
            endretTidspunkt = row.offsetDateTime("endret_tidspunkt"),
            endretAvNavIdent = row.string("endret_av_nav_ident"),
            endringType = SaksbehandlingsperiodeEndringType.valueOf(row.string("endring_type")),
            endringKommentar = row.stringOrNull("endring_kommentar"),
        )
}
