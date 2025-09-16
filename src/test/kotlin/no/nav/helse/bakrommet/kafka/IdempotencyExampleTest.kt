package no.nav.helse.bakrommet.kafka

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Eksempel på hvordan konsumenter kan bruke idempotency headers
 * for å håndtere duplikate meldinger
 */
class IdempotencyExampleTest {
    @Test
    fun `eksempel på hvordan konsument kan bruke outbox-id for idempotency`() {
        // Simulerer en Kafka melding med headers
        val sentMessage =
            SentMessage(
                topic = "speilvendt.spillerom-behandlinger",
                key = "person-123",
                value = """{"type": "behandling-opprettet", "personId": "123"}""",
                headers =
                    mapOf(
                        "outbox-id" to "42",
                        "outbox-opprettet" to "2024-01-15T10:30:00Z",
                    ),
            )

        // Konsument kan nå:
        // 1. Hente ut outbox-id fra headers
        val outboxId = sentMessage.headers["outbox-id"]?.toLongOrNull()
        assertEquals(42L, outboxId)

        // 2. Sjekke om denne meldingen allerede er prosessert
        val alleredeProsessert = checkIfAlreadyProcessed(outboxId!!)
        assertTrue(!alleredeProsessert)

        // 3. Prosessere meldingen og lagre at den er prosessert
        markAsProcessed(outboxId)

        // 4. Hvis samme melding kommer igjen, kan den ignoreres
        val alleredeProsessertIgjen = checkIfAlreadyProcessed(outboxId)
        assertTrue(alleredeProsessertIgjen)
    }

    // Simulerer en database eller cache for å holde styr på prosesserte meldinger
    private val processedMessages = mutableSetOf<Long>()

    private fun checkIfAlreadyProcessed(outboxId: Long): Boolean {
        return processedMessages.contains(outboxId)
    }

    private fun markAsProcessed(outboxId: Long) {
        processedMessages.add(outboxId)
    }
}
