package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.kafka.KafkaMelding
import no.nav.helse.bakrommet.kafka.OutboxDao
import no.nav.helse.bakrommet.kafka.OutboxDbRecord
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class OutboxDaoFake : OutboxDao {
    private val idGenerator = AtomicLong(1)
    private val storage = ConcurrentHashMap<Long, OutboxDbRecord>()

    override fun lagreTilOutbox(kafkaMelding: KafkaMelding) {
        val id = idGenerator.getAndIncrement()
        val entry =
            OutboxDbRecord(
                id = id,
                kafkaKey = kafkaMelding.key,
                kafkaPayload = kafkaMelding.payload,
                opprettet = Instant.now(),
                publisert = null,
                topic = kafkaMelding.topic,
            )
        storage[id] = entry
    }

    override fun markerSomPublisert(id: Long) {
        val eksisterende = storage[id] ?: return
        storage[id] = eksisterende.copy(publisert = Instant.now())
    }

    override fun hentAlleUpubliserteEntries(): List<OutboxDbRecord> = storage.values.filter { it.publisert == null }.sortedBy { it.id }
}
