package no.nav.helse.bakrommet.kafka

import java.time.Instant

data class OutboxDbRecord(
    val id: Long,
    val kafkaKey: String,
    val kafkaPayload: String,
    val opprettet: Instant,
    val topic: String,
    val publisert: Instant?,
)

data class KafkaMelding(
    val topic: String,
    val key: String,
    val payload: String,
)

interface OutboxDao {
    fun lagreTilOutbox(kafkaMelding: KafkaMelding)

    fun markerSomPublisert(id: Long)

    fun hentAlleUpubliserteEntries(): List<OutboxDbRecord>
}
