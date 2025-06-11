package no.nav.helse.bakrommet.saksbehandlingsperiode

import kotliquery.Row
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.insert
import no.nav.helse.bakrommet.util.list
import no.nav.helse.bakrommet.util.single
import java.time.Instant
import java.util.*
import javax.sql.DataSource

data class Dokument(
    val id: UUID = UUID.randomUUID(),
    val dokumentType: String,
    val eksternId: String?,
    val innhold: String,
    val opprettet: Instant = Instant.now(),
    val request: Kildespor,
    val opprettetForBehandling: UUID,
)

class DokumentDao(private val dataSource: DataSource) {
    fun opprettDokument(dokument: Dokument): Dokument {
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
            "request" to dokument.request.kilde,
            "opprettet_for_behandling" to dokument.opprettetForBehandling,
        )
        return hentDokument(dokument.id)!!
    }

    fun hentDokument(id: UUID): Dokument? =
        dataSource.single(
            """
            select * from dokument where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::dokumentFraRow,
        )

    fun hentDokumenterFor(behandlingId: UUID): List<Dokument> =
        dataSource.list(
            """
            select * from dokument where opprettet_for_behandling = :behandling_id
            """.trimIndent(),
            "behandling_id" to behandlingId,
            mapper = ::dokumentFraRow,
        )

    private fun dokumentFraRow(row: Row) =
        Dokument(
            id = row.uuid("id"),
            dokumentType = row.string("dokument_type"),
            eksternId = row.stringOrNull("ekstern_id"),
            innhold = row.string("innhold"),
            opprettet = row.instant("opprettet"),
            request = Kildespor(kilde = row.string("request")),
            opprettetForBehandling = row.uuid("opprettet_for_behandling"),
        )
}
