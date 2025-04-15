package no.nav.helse.spotlight.river

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spotlight.db.TransactionManager
import no.nav.helse.spotlight.slack.SlackClient

class KlokkaSeksHverdagerRiver(
    private val transactionManager: TransactionManager,
    private val slackClient: SlackClient,
) : AbstractSimpleRiver("hel_time", "post_kommandokjeder_til_slack") {
    override fun precondition(message: JsonMessage) {
        message.requireValue("time", 6)
        message.requireAny("ukedag", listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"))
    }

    override fun håndter(
        message: JsonMessage,
        partisjonsnøkkel: String?,
    ) {
        logg.info("Klokka er 6 🐔. Forteller om kommandokjeder som sitter fast på slack.")
        val kommandokjeder =
            transactionManager.transaction { dao ->
                dao.finnAlleEldreEnnEnHalvtime().filter { it.sistSuspenderteSti.antallGangerPåminnet > 0 }
            }
        logg.info("Fant ${kommandokjeder.size} kommandokjeder som sitter fast")
        if (kommandokjeder.isEmpty()) {
            slackClient.sendGladmelding()
        } else {
            slackClient.sendMeldingOmKommandokjederSomSitterFast(kommandokjeder)
        }
    }
}
