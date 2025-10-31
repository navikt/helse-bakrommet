package no.nav.helse.bakrommet.fakedaos

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.hentSessionid
import no.nav.helse.bakrommet.kafka.KafkaMelding
import no.nav.helse.bakrommet.kafka.OutboxDao
import no.nav.helse.bakrommet.kafka.OutboxEntry
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class OutboxDaoFake : OutboxDao {
    // Map av sessionId -> idGenerator og storage
    private val sessionData = ConcurrentHashMap<String, Pair<AtomicLong, ConcurrentHashMap<Long, OutboxEntry>>>()

    private fun getSessionData(): Pair<AtomicLong, ConcurrentHashMap<Long, OutboxEntry>> =
        runBlocking {
            val sessionId = hentSessionid()

            sessionData.getOrPut(sessionId) { AtomicLong(1) to ConcurrentHashMap() }
        }

    private val idGenerator: AtomicLong
        get() = getSessionData().first

    private val storage: ConcurrentHashMap<Long, OutboxEntry>
        get() = getSessionData().second

    override suspend fun lagreTilOutbox(kafkaMelding: KafkaMelding) {
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

    override suspend fun markerSomPublisert(id: Long) {
        val eksisterende = storage[id] ?: return
        storage[id] = eksisterende.copy(publisert = Instant.now())
    }

    override suspend fun hentAlleUpubliserteEntries(): List<OutboxEntry> = storage.values.filter { it.publisert == null }.sortedBy { it.id }
}
