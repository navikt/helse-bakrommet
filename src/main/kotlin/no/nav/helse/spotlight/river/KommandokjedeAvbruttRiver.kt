package no.nav.helse.spotlight.river

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spotlight.db.TransactionManager
import no.nav.helse.spotlight.withMDC
import java.util.*

class KommandokjedeAvbruttRiver(
    private val transactionManager: TransactionManager,
) : AbstractSimpleRiver("kommandokjede_avbrutt") {
    override fun validate(message: JsonMessage) {
        message.requireKey("commandContextId")
    }

    override fun håndter(
        message: JsonMessage,
        partisjonsnøkkel: String?,
    ) {
        val commandContextId = UUID.fromString(message["commandContextId"].asText())
        withMDC(mapOf("commandContextId" to commandContextId)) {
            logg.info("Kommandokjeden er avbrutt")
            transactionManager.transaction { dao ->
                if (dao.finn(commandContextId) == null) {
                    logg.info("Kommandokjeden var aldri suspendert")
                } else {
                    dao.slett(commandContextId)
                }
            }
        }
    }
}
