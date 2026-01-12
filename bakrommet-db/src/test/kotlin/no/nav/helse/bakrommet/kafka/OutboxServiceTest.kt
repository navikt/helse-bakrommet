package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.db.dao.OutboxDaoPg
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutboxServiceTest {
    private val dataSource = TestDataSource.dbModule.dataSource
    private lateinit var outboxDao: OutboxDao
    private lateinit var fakeKafkaProducer: FakeMeldingProducer
    private lateinit var outboxService: OutboxService

    private companion object {
        private const val SPILLEROM_BEHANDLINGER_TOPIC = "speilvendt.spillerom-behandlinger"
        private const val UTBETALINGER_TOPIC = "speilvendt.sykepenger-spillerom-utbetalinger"
    }

    @BeforeEach
    fun setup() {
        TestDataSource.resetDatasource()
        outboxDao = OutboxDaoPg(dataSource)
        fakeKafkaProducer = FakeMeldingProducer()
        outboxService = OutboxService(outboxDao = outboxDao, kafkaProducer = fakeKafkaProducer, lockingDataSource = null)
    }

    @AfterEach
    fun cleanup() {
        fakeKafkaProducer.close()
    }

    @Test
    fun `skal prosessere upubliserte meldinger og sende til Kafka`() {
        // Gitt: En upublisert melding i outbox
        val kafkaMelding =
            KafkaMelding(
                topic = SPILLEROM_BEHANDLINGER_TOPIC,
                key = "test-key-123",
                payload = """{"type": "test", "data": "test-data"}""",
            )
        outboxDao.lagreTilOutbox(kafkaMelding)

        // Når: OutboxService prosesserer meldingene
        val antallProsessert = outboxService.prosesserOutbox()

        // Så: Meldingen skal være sendt til Kafka og markert som publisert
        assertEquals(1, antallProsessert)
        assertEquals(1, fakeKafkaProducer.getSentMessages().size)

        val sentMessage = fakeKafkaProducer.getSentMessages().first()
        assertEquals(SPILLEROM_BEHANDLINGER_TOPIC, sentMessage.topic)
        assertEquals("test-key-123", sentMessage.key)
        assertEquals("""{"type": "test", "data": "test-data"}""", sentMessage.value)

        // Verifiser at headers er satt
        assertTrue(sentMessage.headers.containsKey("outbox-id"))
        assertTrue(sentMessage.headers.containsKey("outbox-opprettet"))

        // Verifiser at meldingen er markert som publisert
        val upubliserteMeldinger = outboxDao.hentAlleUpubliserteEntries()
        assertTrue(upubliserteMeldinger.isEmpty())
    }

    @Test
    fun `skal håndtere flere upubliserte meldinger`() {
        // Gitt: Flere upubliserte meldinger
        outboxDao.lagreTilOutbox(KafkaMelding(SPILLEROM_BEHANDLINGER_TOPIC, "key1", """{"test-payload": true}"""))
        outboxDao.lagreTilOutbox(KafkaMelding(SPILLEROM_BEHANDLINGER_TOPIC, "key2", """{"test-payload": true}"""))
        outboxDao.lagreTilOutbox(KafkaMelding(SPILLEROM_BEHANDLINGER_TOPIC, "key3", """{"test-payload": true}"""))

        // Når: OutboxService prosesserer meldingene
        val antallProsessert = outboxService.prosesserOutbox()

        // Så: Alle meldingene skal være prosessert
        assertEquals(3, antallProsessert)
        assertEquals(3, fakeKafkaProducer.getSentMessages().size)

        val upubliserteMeldinger = outboxDao.hentAlleUpubliserteEntries()
        assertTrue(upubliserteMeldinger.isEmpty())
    }

    @Test
    fun `skal støtte flere forskjellige topic`() {
        outboxDao.lagreTilOutbox(KafkaMelding(SPILLEROM_BEHANDLINGER_TOPIC, "key-standard", """{"test-payload": true}"""))
        outboxDao.lagreTilOutbox(KafkaMelding(UTBETALINGER_TOPIC, "key-annen", """{"test-payload": true}"""))

        val antallProsessert = outboxService.prosesserOutbox()

        assertEquals(2, antallProsessert)
        val sentMessages = fakeKafkaProducer.getSentMessages()
        assertEquals(listOf(SPILLEROM_BEHANDLINGER_TOPIC, UTBETALINGER_TOPIC), sentMessages.map { it.topic })
        assertEquals(listOf("key-standard", "key-annen"), sentMessages.map { it.key })
    }

    @Test
    fun `skal returnere 0 når ingen upubliserte meldinger finnes`() {
        // Når: OutboxService prosesserer uten meldinger
        val antallProsessert = outboxService.prosesserOutbox()

        // Så: Ingen meldinger skal være prosessert
        assertEquals(0, antallProsessert)
        assertTrue(fakeKafkaProducer.getSentMessages().isEmpty())
    }

    @Test
    fun `skal håndtere feil ved Kafka-sending uten å markere som publisert`() {
        // Gitt: En upublisert melding og en feilende Kafka producer
        outboxDao.lagreTilOutbox(KafkaMelding(SPILLEROM_BEHANDLINGER_TOPIC, "test-key", """{"test-payload": true}"""))

        val feilendeProducer =
            object : MeldingProducer {
                override fun send(
                    topic: String,
                    key: String,
                    value: String,
                    headers: Map<String, String>,
                ): CompletableFuture<Unit> = CompletableFuture.failedFuture(RuntimeException("Kafka feil"))

                override fun close() {}
            }

        val outboxServiceMedFeil = OutboxService(outboxDao = outboxDao, kafkaProducer = feilendeProducer, lockingDataSource = null)

        // Når: OutboxService prosesserer meldingen
        val antallProsessert = outboxServiceMedFeil.prosesserOutbox()

        // Så: Ingen meldinger skal være prosessert og meldingen skal fortsatt være upublisert
        assertEquals(0, antallProsessert)

        val upubliserteMeldinger = outboxDao.hentAlleUpubliserteEntries()
        assertEquals(1, upubliserteMeldinger.size)
        assertEquals("test-key", upubliserteMeldinger.first().kafkaKey)
    }

    @Test
    fun `skal prosessere meldinger i riktig rekkefølge basert på id`() {
        // Gitt: Meldinger lagt til i tilfeldig rekkefølge
        outboxDao.lagreTilOutbox(KafkaMelding(SPILLEROM_BEHANDLINGER_TOPIC, "key3", """{"test-payload": true}"""))
        outboxDao.lagreTilOutbox(KafkaMelding(SPILLEROM_BEHANDLINGER_TOPIC, "key1", """{"test-payload": true}"""))
        outboxDao.lagreTilOutbox(KafkaMelding(SPILLEROM_BEHANDLINGER_TOPIC, "key2", """{"test-payload": true}"""))

        // Når: OutboxService prosesserer meldingene
        outboxService.prosesserOutbox()

        // Så: Meldingene skal være sendt i riktig rekkefølge (basert på id)
        val sentMessages = fakeKafkaProducer.getSentMessages()
        assertEquals(3, sentMessages.size)

        // Verifiser at meldingene er sendt i riktig rekkefølge
        assertEquals("key3", sentMessages[0].key) // Første melding (id=1)
        assertEquals("key1", sentMessages[1].key) // Andre melding (id=2)
        assertEquals("key2", sentMessages[2].key) // Tredje melding (id=3)
    }
}
