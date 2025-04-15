package no.nav.helse.spotlight.slack

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spotlight.Configuration
import no.nav.helse.spotlight.SuspendertKommandokjede
import no.nav.helse.spotlight.objectMapper
import no.nav.helse.spotlight.slack.SlackMeldingsbygger.byggTrådMedAttachments
import no.nav.helse.spotlight.slack.SlackMeldingsbygger.gladmelding
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class SlackClient(private val configuration: Configuration.Slack) {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    fun sendGladmelding() {
        logg.info("Poster gladmelding til Slack")
        post(
            mapOf(
                "channel" to configuration.channel,
                "text" to gladmelding,
            ),
        )
    }

    fun sendMeldingOmKommandokjederSomSitterFast(kommandokjeder: List<SuspendertKommandokjede>) {
        postSomTråd(byggTrådMedAttachments(kommandokjeder))
    }

    private fun postSomTråd(attachmentsTråd: List<String>) {
        logg.info("Poster melding til Slack med kommandokjeder som sitter fast")
        val response =
            post(
                mapOf(
                    "channel" to configuration.channel,
                    "attachments" to attachmentsTråd.first(),
                ),
            )
        val attachmentsSomMåITråd = attachmentsTråd.drop(1)
        if (attachmentsSomMåITråd.isNotEmpty()) {
            val threadTs =
                response["ts"]?.asText()
                    ?: error("Fikk ingen tråd-ID i svar fra Slack, kan ikke poste resterende kommandokjeder")
            attachmentsSomMåITråd.forEach { attachments ->
                logg.info("Poster melding i tråd til Slack med ytterligere kommandokjeder som sitter fast")
                post(
                    mapOf(
                        "channel" to configuration.channel,
                        "attachments" to attachments,
                        "thread_ts" to threadTs,
                    ),
                )
            }
        }
    }

    private fun post(payload: Map<String, String>): JsonNode =
        HttpClient.newHttpClient().use { client ->
            val response =
                client.send(
                    HttpRequest.newBuilder()
                        .uri(URI(configuration.url))
                        .timeout(Duration.ofSeconds(50))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("Authorization", "Bearer ${configuration.accessToken}")
                        .header("User-Agent", "navikt/spotlight")
                        .method("POST", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )

            val statusCode = response.statusCode()
            val responseBody = response.body()
            logg.debug("Respons fra slack: code=$statusCode")
            sikkerlogg.debug("Respons fra slack: code=$statusCode body=$responseBody")

            if (statusCode !in 200..299) {
                error("Fikk kode $statusCode i retur fra Slack")
            }

            return objectMapper.readTree(responseBody)
        }
}
