package no.nav.helse.bakrommet

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.Instant

data class OutboxEntryResponse(
    val id: Long,
    val kafkaKey: String,
    val payload: JsonNode,
    val opprettet: Instant,
    val publisert: Instant?,
    val topic: String,
)

fun Route.demoOutboxRoute() {
    get("/v1/demo/kafkaoutbox") {
        val outboxDao =
            sessionsDaoer[hentSession()]?.outboxDao
                ?: throw IllegalStateException("Ingen outboxDao funnet for session")

        val entries = outboxDao.hentAlleUpubliserteEntries()
        val response =
            entries.map { entry ->
                OutboxEntryResponse(
                    id = entry.id,
                    kafkaKey = entry.kafkaKey,
                    payload = entry.kafkaPayload.asJsonNode(),
                    opprettet = entry.opprettet,
                    publisert = entry.publisert,
                    topic = entry.topic,
                )
            }

        call.respondText(response.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
    }
}
