package no.nav.helse.bakrommet.kafka

import java.util.concurrent.CompletableFuture

class FakeMeldingProducer : MeldingProducer {
    private val sentMessages = mutableMapOf<String, MutableList<SentMessage>>()

    override fun send(
        topic: String,
        key: String,
        value: String,
        headers: Map<String, String>,
    ): CompletableFuture<Unit> {
        sentMessages.getOrPut(topic) { mutableListOf() }.add(SentMessage(topic, key, value, headers))
        return CompletableFuture.completedFuture(Unit)
    }

    override fun close() {
        // Ingen implementasjon nødvendig for fake
    }

    fun getSentMessages(): Map<String, List<SentMessage>> = sentMessages.toMap()
}

data class SentMessage(
    val topic: String,
    val key: String,
    val value: String,
    val headers: Map<String, String>,
)
