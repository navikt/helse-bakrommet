package no.nav.helse.spotlight

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

object KafkaMeldingsbygger {
    fun byggKommandokjedePåminnelse(kommandokjede: SuspendertKommandokjede): String =
        JsonMessage.newMessage(
            "kommandokjede_påminnelse",
            mapOf(
                "commandContextId" to kommandokjede.commandContextId,
                "meldingId" to kommandokjede.sisteMeldingId,
            ),
        ).toJson()
}
