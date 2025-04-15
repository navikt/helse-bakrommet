package no.nav.helse.spotlight.river

import no.nav.helse.spotlight.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

class KommandokjedeAvbruttRiverIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `Sletter suspendert kommandokjede når kommandokjede_avbrutt leses inn`() {
        // Given:
        val commandContextId = lagretKommandokjede().commandContextId

        // When:
        testRapid.sendTestMessage(
            """
            {
              "@event_name": "kommandokjede_avbrutt",
              "commandContextId": "$commandContextId",
              "meldingId": "${UUID.randomUUID()}"
            }
            """.trimIndent(),
        )

        // Then:
        assertNull(dao.finn(commandContextId))
    }
}
