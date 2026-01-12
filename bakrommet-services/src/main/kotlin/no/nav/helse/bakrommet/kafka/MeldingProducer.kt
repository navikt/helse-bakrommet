package no.nav.helse.bakrommet.kafka

import java.util.concurrent.CompletableFuture

interface MeldingProducer {
    fun send(
        topic: String,
        key: String,
        value: String,
        headers: Map<String, String> = emptyMap(),
    ): CompletableFuture<Unit>

    fun close()
}
