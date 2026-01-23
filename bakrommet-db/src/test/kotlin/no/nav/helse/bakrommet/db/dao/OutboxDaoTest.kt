package no.nav.helse.bakrommet.db.dao

import no.nav.helse.bakrommet.db.DBTestFixture
import no.nav.helse.bakrommet.kafka.KafkaMelding
import no.nav.helse.bakrommet.kafka.OutboxDbRecord
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class OutboxDaoTest {
    private val dataSource = DBTestFixture.module.dataSource
    private val dao = OutboxDaoPg(dataSource)

    private val etTopic = "${UUID.randomUUID()}"
    private val etAnnetTopic = "${UUID.randomUUID()}"

    @Test
    fun `lagrer en entry i outbox og henter den tilbake`() {
        val kafkaKey = "test-key-123"
        val kafkaPayload = """{"type":"test","data":"some data"}"""

        dao.lagreTilOutbox(KafkaMelding(etTopic, kafkaKey, kafkaPayload))

        val upubliserteEntries = hentAlleUpubliserteEntriesITest()
        Assertions.assertEquals(1, upubliserteEntries.size)

        val entry = upubliserteEntries.first()
        Assertions.assertEquals(etTopic, entry.topic)
        Assertions.assertEquals(kafkaKey, entry.kafkaKey)
        Assertions.assertEquals(kafkaPayload, entry.kafkaPayload)
        Assertions.assertNotNull(entry.opprettet)
        Assertions.assertNull(entry.publisert)
    }

    @Test
    fun `lagrer flere entries og henter dem i riktig rekkefølge`() {
        val entries =
            listOf(
                Triple(etTopic, "key-1", """{"order":1}"""),
                Triple(etAnnetTopic, "key-2", """{"order":2}"""),
                Triple(etTopic, "key-3", """{"order":3}"""),
            )

        entries.forEach { (topic, key, payload) ->
            dao.lagreTilOutbox(KafkaMelding(topic, key, payload))
        }

        val upubliserteEntries = hentAlleUpubliserteEntriesITest()
        Assertions.assertEquals(3, upubliserteEntries.size)

        Assertions.assertEquals("key-1", upubliserteEntries[0].kafkaKey)
        Assertions.assertEquals(etTopic, upubliserteEntries[0].topic)

        Assertions.assertEquals("key-2", upubliserteEntries[1].kafkaKey)
        Assertions.assertEquals(etAnnetTopic, upubliserteEntries[1].topic)

        Assertions.assertEquals("key-3", upubliserteEntries[2].kafkaKey)
        Assertions.assertEquals(etTopic, upubliserteEntries[2].topic)
    }

    @Test
    fun `henter tom liste når ingen upubliserte entries finnes`() {
        val upubliserteEntries = hentAlleUpubliserteEntriesITest()
        Assertions.assertEquals(0, upubliserteEntries.size)
    }

    @Test
    fun `markerer entry som publisert`() {
        val kafkaKey = "test-key-publish"
        val kafkaPayload = """{"status":"ready"}"""

        dao.lagreTilOutbox(KafkaMelding(etTopic, kafkaKey, kafkaPayload))

        val upubliserteEntries = hentAlleUpubliserteEntriesITest()
        Assertions.assertEquals(1, upubliserteEntries.size)
        val entry = upubliserteEntries.first()
        Assertions.assertNull(entry.publisert)

        dao.markerSomPublisert(entry.id)

        val upubliserteEtterPublisering = hentAlleUpubliserteEntriesITest()
        Assertions.assertEquals(0, upubliserteEtterPublisering.size)
    }

    @Test
    fun `lagrer to entries og markerer den første som publisert`() {
        val kafkaKey1 = "key-1"
        val kafkaPayload1 = """{"message":"first"}"""
        val kafkaKey2 = "key-2"
        val kafkaPayload2 = """{"message":"second"}"""

        dao.lagreTilOutbox(KafkaMelding(etTopic, kafkaKey1, kafkaPayload1))
        dao.lagreTilOutbox(KafkaMelding(etAnnetTopic, kafkaKey2, kafkaPayload2))

        val upubliserteEntries = hentAlleUpubliserteEntriesITest()
        Assertions.assertEquals(2, upubliserteEntries.size)

        val førsteEntry = upubliserteEntries[0]
        dao.markerSomPublisert(førsteEntry.id)

        val upubliserteEtterPublisering = hentAlleUpubliserteEntriesITest()
        Assertions.assertEquals(1, upubliserteEtterPublisering.size)

        val gjenværendeEntry = upubliserteEtterPublisering.first()
        Assertions.assertEquals(kafkaKey2, gjenværendeEntry.kafkaKey)
        Assertions.assertEquals(etAnnetTopic, gjenværendeEntry.topic)
        Assertions.assertEquals(kafkaPayload2, gjenværendeEntry.kafkaPayload)
        Assertions.assertNull(gjenværendeEntry.publisert)
    }

    private fun hentAlleUpubliserteEntriesITest(): List<OutboxDbRecord> =
        dao
            .hentAlleUpubliserteEntries()
            // Filtrerer på test topics for å kunne kjøre tester i parallel
            .filter { it.topic in listOf(etTopic, etAnnetTopic) }
}
