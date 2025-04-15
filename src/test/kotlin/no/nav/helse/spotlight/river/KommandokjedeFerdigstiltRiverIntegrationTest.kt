package no.nav.helse.spotlight.river

import no.nav.helse.spotlight.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class KommandokjedeFerdigstiltRiverIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `Sletter suspendert kommandokjede når kommandokjede_ferdigstilt leses inn`() {
        // Given:
        val commandContextId = lagretKommandokjede().commandContextId

        // When:
        testRapid.sendTestMessage(
            """
            {
              "@event_name": "kommandokjede_ferdigstilt",
              "commandContextId": "$commandContextId",
              "meldingId": "${UUID.randomUUID()}",
              "command": "EnCommand",
              "@opprettet": "${LocalDateTime.now()}"
            }
            """.trimIndent(),
        )

        // Then:
        assertNull(dao.finn(commandContextId))
    }
}
