package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.db.TestDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OutboxDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
    }

    @Test
    fun `lagrer en entry i outbox og henter den tilbake`() {
        val dao = OutboxDaoPg(dataSource)
        val kafkaKey = "test-key-123"
        val kafkaPayload = """{"type":"test","data":"some data"}"""

        dao.lagreTilOutbox(KafkaMelding(kafkaKey, kafkaPayload))

        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        assertEquals(1, upubliserteEntries.size)

        val entry = upubliserteEntries.first()
        assertEquals(kafkaKey, entry.kafkaKey)
        assertEquals(kafkaPayload, entry.kafkaPayload)
        assertNotNull(entry.opprettet)
        assertNull(entry.publisert)
    }

    @Test
    fun `lagrer flere entries og henter dem i riktig rekkefølge`() {
        val dao = OutboxDaoPg(dataSource)

        val entry1 = Pair("key-1", """{"order":1}""")
        val entry2 = Pair("key-2", """{"order":2}""")
        val entry3 = Pair("key-3", """{"order":3}""")

        dao.lagreTilOutbox(KafkaMelding(entry1.first, entry1.second))
        dao.lagreTilOutbox(KafkaMelding(entry2.first, entry2.second))
        dao.lagreTilOutbox(KafkaMelding(entry3.first, entry3.second))

        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        assertEquals(3, upubliserteEntries.size)

        // Verify they are ordered by id (sequential)
        assertEquals("key-1", upubliserteEntries[0].kafkaKey)
        assertEquals("key-2", upubliserteEntries[1].kafkaKey)
        assertEquals("key-3", upubliserteEntries[2].kafkaKey)
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

        dao.lagreTilOutbox(KafkaMelding(kafkaKey, kafkaPayload))

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

        dao.lagreTilOutbox(KafkaMelding(kafkaKey1, kafkaPayload1))
        dao.lagreTilOutbox(KafkaMelding(kafkaKey2, kafkaPayload2))

        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        assertEquals(2, upubliserteEntries.size)

        val førsteEntry = upubliserteEntries[0]
        dao.markerSomPublisert(førsteEntry.id)

        val upubliserteEtterPublisering = dao.hentAlleUpubliserteEntries()
        assertEquals(1, upubliserteEtterPublisering.size)

        val gjenværendeEntry = upubliserteEtterPublisering.first()
        assertEquals(kafkaKey2, gjenværendeEntry.kafkaKey)
        assertEquals(kafkaPayload2, gjenværendeEntry.kafkaPayload)
        assertNull(gjenværendeEntry.publisert)
    }
}
