package no.nav.helse.bakrommet.kafka

import java.util.concurrent.CompletableFuture

interface KafkaProducerInterface {
    fun send(
        topic: String,
        key: String,
        value: String,
        headers: Map<String, String> = emptyMap(),
    ): CompletableFuture<Unit>

    fun close()
}
