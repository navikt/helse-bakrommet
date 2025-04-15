package no.nav.helse.spotlight.river

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spotlight.KafkaMeldingsbygger.byggKommandokjedePåminnelse
import no.nav.helse.spotlight.SuspendertKommandokjede
import no.nav.helse.spotlight.db.TransactionManager
import no.nav.helse.spotlight.withMDC
import java.time.LocalTime

class HverHalvtimeRiver(
    private val transactionManager: TransactionManager,
    private val rapidsConnection: RapidsConnection,
) : AbstractSimpleRiver("halv_time", "påminn_kommandokjeder_som_sitter_fast") {
    override fun precondition(message: JsonMessage) {
        message.require("klokkeslett") {
            check(it.asLocalTime().isAfter(LocalTime.now().minusMinutes(30)))
        }
    }

    override fun håndter(
        message: JsonMessage,
        partisjonsnøkkel: String?,
    ) {
        val kommandokjeder =
            transactionManager.transaction { dao ->
                dao.finnAlleEldreEnnEnHalvtime()
                    .also { logg.info("Fant ${it.size} kommandokjeder som sitter fast") }
                    .map { it.medØktAntallGangerPåminnet() }
                    .onEach {
                        withMDC(mapOf("commandContextId" to it.commandContextId)) {
                            dao.update(it)
                        }
                    }
            }
        kommandokjeder.forEach {
            withMDC(mapOf("commandContextId" to it.commandContextId)) {
                logg.info("Sender påminnelse for kommandokjede")
                rapidsConnection.publish(key = it.sistePartisjonsnøkkel, message = byggKommandokjedePåminnelse(it))
            }
        }
    }
}

private fun SuspendertKommandokjede.medØktAntallGangerPåminnet(): SuspendertKommandokjede =
    copy(
        totaltAntallGangerPåminnet = totaltAntallGangerPåminnet + 1,
        sistSuspenderteSti =
            sistSuspenderteSti.copy(
                antallGangerPåminnet = sistSuspenderteSti.antallGangerPåminnet + 1,
            ),
    )

private fun JsonNode.asLocalTime() = LocalTime.parse(asText())
