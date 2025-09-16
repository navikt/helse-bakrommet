package no.nav.helse.bakrommet.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.StringSerializer
import java.util.concurrent.CompletableFuture

class KafkaProducerImpl : KafkaProducerInterface {
    private val producer: KafkaProducer<String, String> =
        KafkaProducer<String, String>(
            KafkaUtils.getAivenKafkaConfig("helse-bakrommet-kafka-producer")
                .toProducerConfig(StringSerializer::class, StringSerializer::class),
        )

    override fun send(
        topic: String,
        key: String,
        value: String,
        headers: Map<String, String>,
    ): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()

        val recordHeaders = RecordHeaders()
        headers.forEach { (headerKey, headerValue) ->
            recordHeaders.add(headerKey, headerValue.toByteArray())
        }

        val record = ProducerRecord(topic, null, key, value, recordHeaders)
        producer.send(record) { _, exception ->
            if (exception != null) {
                future.completeExceptionally(exception)
            } else {
                future.complete(Unit)
            }
        }

        return future
    }

    override fun close() {
        producer.close()
    }
}
