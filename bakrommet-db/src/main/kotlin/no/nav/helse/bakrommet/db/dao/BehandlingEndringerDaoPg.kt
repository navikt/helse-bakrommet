package no.nav.helse.bakrommet.db.dao

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.BehandlingEndringerDao
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndring
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringType
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import java.util.UUID
import javax.sql.DataSource

class BehandlingEndringerDaoPg private constructor(
    private val db: QueryRunner,
) : BehandlingEndringerDao {
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
            "behandling_id" to hist.behandlingId,
            "status" to hist.status.name,
            "beslutter_nav_ident" to hist.beslutterNavIdent,
            "endret_tidspunkt" to hist.endretTidspunkt,
            "endret_av_nav_ident" to hist.endretAvNavIdent,
            "endring_type" to hist.endringType.name,
            "endring_kommentar" to hist.endringKommentar,
        )
    }

    override fun hentEndringerFor(behandlingId: UUID): List<SaksbehandlingsperiodeEndring> =
        db.list(
            """
            select *
              from behandling_endringer
              where behandling_id = :behandling_id
              order by id
            """.trimIndent(),
            "behandling_id" to behandlingId,
        ) { rowTilHistorikk(it) }

    private fun rowTilHistorikk(row: Row) =
        SaksbehandlingsperiodeEndring(
            behandlingId = row.uuid("behandling_id"),
            status = BehandlingStatus.valueOf(row.string("status")),
            beslutterNavIdent = row.stringOrNull("beslutter_nav_ident"),
            endretTidspunkt = row.offsetDateTime("endret_tidspunkt"),
            endretAvNavIdent = row.string("endret_av_nav_ident"),
            endringType = SaksbehandlingsperiodeEndringType.valueOf(row.string("endring_type")),
            endringKommentar = row.stringOrNull("endring_kommentar"),
        )
}
