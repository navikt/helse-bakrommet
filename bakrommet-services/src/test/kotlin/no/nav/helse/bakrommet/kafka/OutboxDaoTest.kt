package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.db.TestDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OutboxDaoTest {
    private val dataSource = TestDataSource.dbModule.dataSource

    private companion object {
        private const val STANDARD_TOPIC = "speilvendt.spillerom-behandlinger"
        private const val ANNEN_TOPIC = "speilvendt.spillerom-andre-meldinger"
    }

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
    }

    @Test
    fun `lagrer en entry i outbox og henter den tilbake`() {
        val dao = OutboxDaoPg(dataSource)
        val kafkaKey = "test-key-123"
        val kafkaPayload = """{"type":"test","data":"some data"}"""

        dao.lagreTilOutbox(KafkaMelding(STANDARD_TOPIC, kafkaKey, kafkaPayload))

        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        assertEquals(1, upubliserteEntries.size)

        val entry = upubliserteEntries.first()
        assertEquals(STANDARD_TOPIC, entry.topic)
        assertEquals(kafkaKey, entry.kafkaKey)
        assertEquals(kafkaPayload, entry.kafkaPayload)
        assertNotNull(entry.opprettet)
        assertNull(entry.publisert)
    }

    @Test
    fun `lagrer flere entries og henter dem i riktig rekkefølge`() {
        val dao = OutboxDaoPg(dataSource)

        val entries =
            listOf(
                Triple(STANDARD_TOPIC, "key-1", """{"order":1}"""),
                Triple(ANNEN_TOPIC, "key-2", """{"order":2}"""),
                Triple(STANDARD_TOPIC, "key-3", """{"order":3}"""),
            )

        entries.forEach { (topic, key, payload) ->
            dao.lagreTilOutbox(KafkaMelding(topic, key, payload))
        }

        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        assertEquals(3, upubliserteEntries.size)

        assertEquals("key-1", upubliserteEntries[0].kafkaKey)
        assertEquals(STANDARD_TOPIC, upubliserteEntries[0].topic)

        assertEquals("key-2", upubliserteEntries[1].kafkaKey)
        assertEquals(ANNEN_TOPIC, upubliserteEntries[1].topic)

        assertEquals("key-3", upubliserteEntries[2].kafkaKey)
        assertEquals(STANDARD_TOPIC, upubliserteEntries[2].topic)
    }

    @Test
    fun `henter tom liste når ingen upubliserte entries finnes`() {
        val dao = OutboxDaoPg(dataSource)
        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        assertEquals(0, upubliserteEntries.size)
    }

    @Test
    fun `markerer entry som publisert`() {
        val dao = OutboxDaoPg(dataSource)
        val kafkaKey = "test-key-publish"
        val kafkaPayload = """{"status":"ready"}"""

        dao.lagreTilOutbox(KafkaMelding(STANDARD_TOPIC, kafkaKey, kafkaPayload))

        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        assertEquals(1, upubliserteEntries.size)
        val entry = upubliserteEntries.first()
        assertNull(entry.publisert)

        dao.markerSomPublisert(entry.id)

        val upubliserteEtterPublisering = dao.hentAlleUpubliserteEntries()
        assertEquals(0, upubliserteEtterPublisering.size)
    }

    @Test
    fun `lagrer to entries og markerer den første som publisert`() {
        val dao = OutboxDaoPg(dataSource)
        val kafkaKey1 = "key-1"
        val kafkaPayload1 = """{"message":"first"}"""
        val kafkaKey2 = "key-2"
        val kafkaPayload2 = """{"message":"second"}"""

        dao.lagreTilOutbox(KafkaMelding(STANDARD_TOPIC, kafkaKey1, kafkaPayload1))
        dao.lagreTilOutbox(KafkaMelding(ANNEN_TOPIC, kafkaKey2, kafkaPayload2))

        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        assertEquals(2, upubliserteEntries.size)

        val førsteEntry = upubliserteEntries[0]
        dao.markerSomPublisert(førsteEntry.id)

        val upubliserteEtterPublisering = dao.hentAlleUpubliserteEntries()
        assertEquals(1, upubliserteEtterPublisering.size)

        val gjenværendeEntry = upubliserteEtterPublisering.first()
        assertEquals(kafkaKey2, gjenværendeEntry.kafkaKey)
        assertEquals(ANNEN_TOPIC, gjenværendeEntry.topic)
        assertEquals(kafkaPayload2, gjenværendeEntry.kafkaPayload)
        assertNull(gjenværendeEntry.publisert)
    }
}
