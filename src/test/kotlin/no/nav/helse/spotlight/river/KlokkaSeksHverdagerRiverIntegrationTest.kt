package no.nav.helse.spotlight.river

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.helse.spotlight.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class KlokkaSeksHverdagerRiverIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `Poster gladmelding om ingen kommandokjeder som står fast på slack når klokka er 6`() {
        // Given:
        wireMockSlack.stubFor(post(urlEqualTo("/")).willReturn(ok()))

        // When:
        sendTestMelding()

        // Then:
        wireMockSlack.verify(
            postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo("Bearer ${slackConfiguration.accessToken}"))
                .withHeader("User-Agent", equalTo("navikt/spotlight")),
        )
        val requestJson = wireMockSlack.allServeEvents.first().request.bodyAsString.parseJson()
        assertEquals(
            """
            {
              "channel" : "${slackConfiguration.channel}",
              "text" : ":spotlight: Ingen kommandokjeder sitter fast :spotlight:"
            }
            """.trimIndent().parseJson().toPrettyString(),
            requestJson.toPrettyString(),
        )
    }

    @Test
    fun `Poster en kommandokjede som står fast på slack når klokka er 6`() {
        // Given:
        wireMockSlack.stubFor(post(urlEqualTo("/")).willReturn(ok()))
        val kommandokjede =
            lagretKommandokjede(
                mottattTidspunkt = Instant.now().minusMinutes(31),
                antallGangerPåminnet = 1337,
            )
        checkNotNull(dao.finn(kommandokjede.commandContextId))

        // When:
        sendTestMelding()

        // Then:
        wireMockSlack.verify(
            postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8")),
        )
        val requestJson = wireMockSlack.allServeEvents.first().request.bodyAsString.parseJson()
        assertEquals(slackConfiguration.channel, requestJson["channel"]?.asText())
        assertEquals(
            """
            [ {
              "color" : "#36b528",
              "blocks" : [ {
                "type" : "section",
                "text" : {
                  "type" : "mrkdwn",
                  "text" : ":spotlight: Det er 1 kommandokjede som sitter fast: :spotlight:"
                }
              }, {
                "type" : "section",
                "fields" : [ {
                  "type" : "mrkdwn",
                  "text" : "*commandContextId:*\n<https://logs.adeo.no/app/discover#/?_g=(filters:!(),refreshInterval:(pause:!t,value:60000),time:(from:now-7d%2Fd,to:now))&_a=(columns:!(level,message,envclass,application,pod),filters:!(),hideChart:!f,index:'96e648c0-980a-11e9-830a-e17bbd64b4db',interval:auto,query:(language:kuery,query:%22${kommandokjede.commandContextId}%22),sort:!(!('@timestamp',desc)))|${kommandokjede.commandContextId}>"
                }, {
                  "type" : "mrkdwn",
                  "text" : "*Siste meldingId:*\n<https://logs.adeo.no/app/discover#/?_g=(filters:!(),refreshInterval:(pause:!t,value:60000),time:(from:now-7d%2Fd,to:now))&_a=(columns:!(level,message,envclass,application,pod),filters:!(),hideChart:!f,index:'96e648c0-980a-11e9-830a-e17bbd64b4db',interval:auto,query:(language:kuery,query:%22${kommandokjede.sisteMeldingId}%22),sort:!(!('@timestamp',desc)))|${kommandokjede.sisteMeldingId}>"
                }, {
                  "type" : "mrkdwn",
                  "text" : "*Command:*\n${kommandokjede.command}"
                }, {
                  "type" : "mrkdwn",
                  "text" : "*Sti:*\n${kommandokjede.sistSuspenderteSti.sti}"
                }, {
                  "type" : "mrkdwn",
                  "text" : "*Suspendert siden:*\n${
                kommandokjede.sistSuspenderteSti.førsteTidspunkt.atZone(ZoneId.of("Europe/Oslo"))
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
            }"
                }, {
                  "type" : "mrkdwn",
                  "text" : "*Antall ganger påminnet:*\n${kommandokjede.sistSuspenderteSti.antallGangerPåminnet}"
                } ]
              } ]
            } ]
            """.trimIndent().parseJson().toPrettyString(),
            requestJson["attachments"]?.asText()?.parseJson()?.toPrettyString(),
        )
    }

    @Test
    fun `Poster 80 kommandokjeder fordelt utover tråd`() {
        // Given:
        val ts = UUID.randomUUID().toString()
        wireMockSlack.stubFor(post(urlEqualTo("/")).willReturn(okJson("""{ "ts" : "$ts" }""")))
        (1..80).forEach { index ->
            lagretKommandokjede(
                mottattTidspunkt = Instant.now().minusMinutes(31L + index),
                antallGangerPåminnet = index,
            )
        }

        // When:
        sendTestMelding()

        // Then:
        wireMockSlack.verify(
            2,
            postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8")),
        )

        val førsteRequest = wireMockSlack.allServeEvents[1].request.bodyAsString.parseJson()
        assertNull(førsteRequest["thread_ts"])
        assertEquals(50, førsteRequest["attachments"]?.asText()?.parseJson()?.first()?.get("blocks")?.size())

        val sisteRequest = wireMockSlack.allServeEvents[0].request.bodyAsString.parseJson()
        assertEquals(ts, sisteRequest["thread_ts"]?.asText())
        assertEquals(32, sisteRequest["attachments"]?.asText()?.parseJson()?.first()?.get("blocks")?.size())
    }

    private fun sendTestMelding() {
        testRapid.sendTestMessage(
            """
            {
              "@event_name": "hel_time",
              "time": 6,
              "minutt": 0,
              "klokkeslett": "06:00:02.50230667",
              "dagen": "2024-03-07",
              "ukedag": "THURSDAY"
            }
            """.trimIndent(),
        )
    }

    private val objectMapper = jacksonObjectMapper()

    private fun String.parseJson(): JsonNode = objectMapper.readTree(this)
}
