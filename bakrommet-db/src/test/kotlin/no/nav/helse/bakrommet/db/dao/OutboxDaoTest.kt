package no.nav.helse.bakrommet.db.dao

import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.kafka.KafkaMelding
import org.junit.jupiter.api.Assertions
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
        Assertions.assertEquals(1, upubliserteEntries.size)

        val entry = upubliserteEntries.first()
        Assertions.assertEquals(STANDARD_TOPIC, entry.topic)
        Assertions.assertEquals(kafkaKey, entry.kafkaKey)
        Assertions.assertEquals(kafkaPayload, entry.kafkaPayload)
        Assertions.assertNotNull(entry.opprettet)
        Assertions.assertNull(entry.publisert)
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
        Assertions.assertEquals(3, upubliserteEntries.size)

        Assertions.assertEquals("key-1", upubliserteEntries[0].kafkaKey)
        Assertions.assertEquals(STANDARD_TOPIC, upubliserteEntries[0].topic)

        Assertions.assertEquals("key-2", upubliserteEntries[1].kafkaKey)
        Assertions.assertEquals(ANNEN_TOPIC, upubliserteEntries[1].topic)

        Assertions.assertEquals("key-3", upubliserteEntries[2].kafkaKey)
        Assertions.assertEquals(STANDARD_TOPIC, upubliserteEntries[2].topic)
    }

    @Test
    fun `henter tom liste når ingen upubliserte entries finnes`() {
        val dao = OutboxDaoPg(dataSource)
        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        Assertions.assertEquals(0, upubliserteEntries.size)
    }

    @Test
    fun `markerer entry som publisert`() {
        val dao = OutboxDaoPg(dataSource)
        val kafkaKey = "test-key-publish"
        val kafkaPayload = """{"status":"ready"}"""

        dao.lagreTilOutbox(KafkaMelding(STANDARD_TOPIC, kafkaKey, kafkaPayload))

        val upubliserteEntries = dao.hentAlleUpubliserteEntries()
        Assertions.assertEquals(1, upubliserteEntries.size)
        val entry = upubliserteEntries.first()
        Assertions.assertNull(entry.publisert)

        dao.markerSomPublisert(entry.id)

        val upubliserteEtterPublisering = dao.hentAlleUpubliserteEntries()
        Assertions.assertEquals(0, upubliserteEtterPublisering.size)
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
        Assertions.assertEquals(2, upubliserteEntries.size)

        val førsteEntry = upubliserteEntries[0]
        dao.markerSomPublisert(førsteEntry.id)

        val upubliserteEtterPublisering = dao.hentAlleUpubliserteEntries()
        Assertions.assertEquals(1, upubliserteEtterPublisering.size)

        val gjenværendeEntry = upubliserteEtterPublisering.first()
        Assertions.assertEquals(kafkaKey2, gjenværendeEntry.kafkaKey)
        Assertions.assertEquals(ANNEN_TOPIC, gjenværendeEntry.topic)
        Assertions.assertEquals(kafkaPayload2, gjenværendeEntry.kafkaPayload)
        Assertions.assertNull(gjenværendeEntry.publisert)
    }
}
