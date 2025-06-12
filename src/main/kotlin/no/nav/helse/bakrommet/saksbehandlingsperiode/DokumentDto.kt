package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.asJsonNode
import java.time.Instant
import java.util.*

data class DokumentDto(
    val id: UUID,
    val dokumentType: String,
    val eksternId: String?,
    val innhold: JsonNode,
    val opprettet: Instant,
    val request: Kildespor,
    val opprettetForBehandling: UUID,
)

fun Dokument.tilDto(): DokumentDto =
    DokumentDto(
        id = id,
        dokumentType = dokumentType,
        eksternId = eksternId,
        innhold = innhold.asJsonNode(),
        opprettet = opprettet,
        request = request,
        opprettetForBehandling = opprettetForBehandling,
    ) 
