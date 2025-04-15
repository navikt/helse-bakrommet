package no.nav.helse.spotlight.river

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.spotlight.SuspendertKommandokjede
import no.nav.helse.spotlight.db.TransactionManager
import no.nav.helse.spotlight.withMDC
import java.time.Instant
import java.time.ZoneId
import java.util.*

class KommandokjedeSuspendertRiver(
    private val transactionManager: TransactionManager,
) : AbstractSimpleRiver("kommandokjede_suspendert") {
    override fun precondition(message: JsonMessage) {
        message.forbidValue("command", "OppdaterPersonsnapshotCommand")
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("commandContextId")
        message.requireKey("meldingId")
        message.requireKey("command")
        message.requireKey("sti")
        message.requireKey("@opprettet")
    }

    override fun håndter(
        message: JsonMessage,
        partisjonsnøkkel: String?,
    ) {
        if (partisjonsnøkkel == null) {
            logg.error("Støtter ikke å behandle melding om suspendert kommandokjede som mangler partisjonsnøkkel")
            return
        }
        val commandContextId = UUID.fromString(message["commandContextId"].asText())
        withMDC(mapOf("commandContextId" to commandContextId)) {
            val meldingId = UUID.fromString(message["meldingId"].asText())
            val command = message["command"].asText()
            val sti = message["sti"].toPrettyString()
            val opprettetTidspunkt =
                message["@opprettet"].asLocalDateTime().atZone(ZoneId.of("Europe/Oslo")).toInstant()

            logg.info("Mottok suspendert kommandokjede $command$sti")

            transactionManager.transaction { dao ->
                val eksisterendeKommandokjede = dao.finn(commandContextId)
                if (eksisterendeKommandokjede == null) {
                    logg.info("Dette er en kommandokjede vi ikke tidligere har sett")
                    dao.insert(
                        SuspendertKommandokjede(
                            commandContextId = commandContextId,
                            command = command,
                            førsteTidspunkt = opprettetTidspunkt,
                            sisteTidspunkt = opprettetTidspunkt,
                            sisteMeldingId = meldingId,
                            sistePartisjonsnøkkel = partisjonsnøkkel,
                            totaltAntallGangerPåminnet = 0,
                            sistSuspenderteSti = nySti(sti = sti, førsteTidspunkt = opprettetTidspunkt),
                        ),
                    )
                } else {
                    val oppdatertSti =
                        if (sti == eksisterendeKommandokjede.sistSuspenderteSti.sti) {
                            logg.info("Dette er en ny melding for kjent kommandokjede og sti")
                            eksisterendeKommandokjede.sistSuspenderteSti
                        } else {
                            logg.info(
                                "Kommandokjeden har kommet videre til ny sti " +
                                    "(${eksisterendeKommandokjede.sistSuspenderteSti.sti} -> $sti)",
                            )
                            nySti(sti = sti, førsteTidspunkt = opprettetTidspunkt)
                        }
                    dao.update(
                        eksisterendeKommandokjede.copy(
                            sisteTidspunkt = opprettetTidspunkt,
                            sisteMeldingId = meldingId,
                            sistePartisjonsnøkkel = partisjonsnøkkel,
                            sistSuspenderteSti = oppdatertSti,
                        ),
                    )
                }
            }
        }
    }

    private fun nySti(
        sti: String,
        førsteTidspunkt: Instant,
    ) = SuspendertKommandokjede.Sti(
        sti = sti,
        førsteTidspunkt = førsteTidspunkt,
        antallGangerPåminnet = 0,
    )
}
