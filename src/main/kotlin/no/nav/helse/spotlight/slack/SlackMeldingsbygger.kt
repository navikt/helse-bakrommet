package no.nav.helse.spotlight.slack

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spotlight.SuspendertKommandokjede
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SlackMeldingsbygger {
    val gladmelding = ":spotlight: Ingen kommandokjeder sitter fast :spotlight:"

    fun byggTrådMedAttachments(kommandokjeder: List<SuspendertKommandokjede>): List<String> =
        kommandokjeder.chunked(49).mapIndexed { index, chunk ->
            // Slack APIet støtter bare 50 blocks pr melding. Hvis det er mer enn 50 stuck kommandokjeder
            // postes resterende i tråd.
            chunk.tilAttachments(
                if (index == 0) {
                    ":spotlight: Det er ${kommandokjeder.antallMedBenevning()} som sitter fast: :spotlight:"
                } else {
                    ":the-more-you-know: Fortsettelse: :the-more-you-know:"
                },
            )
        }.map { it.toJsonString() }

    private fun List<SuspendertKommandokjede>.antallMedBenevning() = "$size ${if (size == 1) "kommandokjede" else "kommandokjeder"}"

    private val objectMapper = jacksonObjectMapper()

    private fun Any.toJsonString(): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

    private fun List<SuspendertKommandokjede>.tilAttachments(tittel: String) =
        listOf(
            attachment(
                color = "#36b528",
                blocks =
                    listOf(
                        section(
                            text =
                                markdown(
                                    text = tittel,
                                ),
                        ),
                    ) +
                        map { kommandokjede ->
                            section(
                                fields = kommandokjede.tilFields(),
                            )
                        },
            ),
        )

    private fun SuspendertKommandokjede.tilFields(): List<Map<String, String>> =
        listOf(
            markdownMedOverskrift(
                overskrift = "commandContextId:",
                tekst = adeoQueryLink(commandContextId.toString()),
            ),
            markdownMedOverskrift(
                overskrift = "Siste meldingId:",
                tekst = adeoQueryLink(sisteMeldingId.toString()),
            ),
            markdownMedOverskrift(
                overskrift = "Command:",
                tekst = command,
            ),
            markdownMedOverskrift(
                overskrift = "Sti:",
                tekst = sistSuspenderteSti.sti,
            ),
            markdownMedOverskrift(
                overskrift = "Suspendert siden:",
                tekst = sistSuspenderteSti.førsteTidspunkt.formattert(),
            ),
            markdownMedOverskrift(
                overskrift = "Antall ganger påminnet:",
                tekst = sistSuspenderteSti.antallGangerPåminnet.toString(),
            ),
        )

    private fun markdownMedOverskrift(
        overskrift: String,
        tekst: String,
    ) = markdown(
        text = "${Markdown.bold(overskrift)}\n$tekst",
    )

    private fun attachment(
        color: String,
        blocks: List<Map<String, Any>>,
    ) = mapOf(
        "color" to color,
        "blocks" to blocks,
    )

    private fun section(text: Map<String, Any?>) =
        mapOf(
            "type" to "section",
            "text" to text,
        )

    private fun section(fields: List<Map<String, Any?>>) =
        mapOf(
            "type" to "section",
            "fields" to fields,
        )

    private fun markdown(text: String) =
        mapOf(
            "type" to "mrkdwn",
            "text" to text,
        )

    private fun adeoQueryLink(query: String) = Markdown.link(adeoUrlMedQuery(query), query)

    private fun adeoUrlMedQuery(query: String) =
        "https://logs.adeo.no/app/discover#/?_g=(filters:!(),refreshInterval:(pause:!t,value:60000),time:(from:now-7d%2Fd,to:now))&_a=(columns:!(level,message,envclass,application,pod),filters:!(),hideChart:!f,index:'96e648c0-980a-11e9-830a-e17bbd64b4db',interval:auto,query:(language:kuery,query:%22$query%22),sort:!(!('@timestamp',desc)))"

    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    private fun Instant.formattert(): String = atZone(ZoneId.of("Europe/Oslo")).format(formatter)

    private object Markdown {
        fun bold(text: String) = "*$text*"

        fun link(
            url: String,
            displayText: String,
        ) = "<$url|$displayText>"
    }
}
