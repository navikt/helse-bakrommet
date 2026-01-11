package no.nav.helse.bakrommet.kafka

import java.util.concurrent.CompletableFuture

class FakeKafkaProducer : KafkaProducerInterface {
    private val sentMessages = mutableListOf<SentMessage>()

    override fun send(
        topic: String,
        key: String,
        value: String,
        headers: Map<String, String>,
    ): CompletableFuture<Unit> {
        sentMessages.add(SentMessage(topic, key, value, headers))
        return CompletableFuture.completedFuture(Unit)
    }

    override fun close() {
        // Ingen implementasjon nødvendig for fake
    }

    fun getSentMessages(): List<SentMessage> = sentMessages.toList()

    fun clearSentMessages() = sentMessages.clear()
}

data class SentMessage(
    val topic: String,
    val key: String,
    val value: String,
    val headers: Map<String, String>,
)
