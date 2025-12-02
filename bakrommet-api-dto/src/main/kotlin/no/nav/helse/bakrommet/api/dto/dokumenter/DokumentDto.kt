package no.nav.helse.bakrommet.api.dto.dokumenter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse
import java.time.Instant
import java.util.*

data class DokumentDto(
    val id: UUID,
    val dokumentType: String,
    val eksternId: String?,
    val innhold: JsonNode,
    val opprettet: Instant,
    val request: KildesporDto,
) : ApiResponse
