package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.util.insert
import no.nav.helse.bakrommet.util.list
import java.time.Instant
import java.util.*
import javax.sql.DataSource

data class Dokument(
    val id: UUID,
    val dokumentType: String,
    val eksternId: String?,
    val innhold: String,
    val opprettet: Instant,
    val request: String,
    val opprettetForBehandling: UUID,
)

class DokumentDao(private val dataSource: DataSource) {
    fun opprettDokument(dokument: Dokument) {
        dataSource.insert(
            """
            insert into dokument
                (id, dokument_type, ekstern_id, innhold, opprettet, request, opprettet_for_behandling)
            values
                (:id, :dokument_type, :ekstern_id, :innhold, :opprettet, :request, :opprettet_for_behandling)
            """.trimIndent(),
            "id" to dokument.id,
            "dokument_type" to dokument.dokumentType,
            "ekstern_id" to dokument.eksternId,
            "innhold" to dokument.innhold,
            "opprettet" to dokument.opprettet,
            "request" to dokument.request,
            "opprettet_for_behandling" to dokument.opprettetForBehandling,
        )
    }

    fun hentDokumenterFor(behandlingId: UUID): List<Dokument> =
        dataSource.list(
            """
            select * from dokument where opprettet_for_behandling = :behandling_id
            """.trimIndent(),
            "behandling_id" to behandlingId,
        ) { row ->
            Dokument(
                id = row.uuid("id"),
                dokumentType = row.string("dokument_type"),
                eksternId = row.stringOrNull("ekstern_id"),
                innhold = row.string("innhold"),
                opprettet = row.instant("opprettet"),
                request = row.string("request"),
                opprettetForBehandling = row.uuid("opprettet_for_behandling"),
            )
        }
}
