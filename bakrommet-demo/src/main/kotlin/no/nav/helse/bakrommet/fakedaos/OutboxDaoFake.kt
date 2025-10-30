package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.kafka.KafkaMelding
import no.nav.helse.bakrommet.kafka.OutboxDao
import no.nav.helse.bakrommet.kafka.OutboxEntry
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class OutboxDaoFake : OutboxDao {
    private val idGenerator = AtomicLong(1)
    private val storage = ConcurrentHashMap<Long, OutboxEntry>()

    override fun lagreTilOutbox(kafkaMelding: KafkaMelding) {
        val id = idGenerator.getAndIncrement()
        val entry =
            OutboxEntry(
                id = id,
                kafkaKey = kafkaMelding.kafkaKey,
                kafkaPayload = kafkaMelding.kafkaPayload,
                opprettet = Instant.now(),
                publisert = null,
            )
        storage[id] = entry
    }

    override fun markerSomPublisert(id: Long) {
        val eksisterende = storage[id] ?: return
        storage[id] = eksisterende.copy(publisert = Instant.now())
    }

    override fun hentAlleUpubliserteEntries(): List<OutboxEntry> = storage.values.filter { it.publisert == null }.sortedBy { it.id }
}
