package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.LåsProvider
import no.nav.helse.bakrommet.fakedaos.OutboxDaoFake
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutboxServiceTest {
    private val outboxDaoFake = OutboxDaoFake()
    private val fakeKafkaProducer = FakeMeldingProducer()
    private val spilleromBehandlingerTopic = "spillerom-behandlinger"
    private val utbetalingerTopic = "utbetalinger"

    private val låsProvider =
        object : LåsProvider {
            override fun <T : Any> kjørMedLås(
                iMinst: Duration,
                maksimalt: Duration,
                block: () -> T,
            ): T = block()
        }

    private val outboxService = OutboxService(outboxDao = outboxDaoFake, kafkaProducer = fakeKafkaProducer, låsProvider = låsProvider)

    @AfterEach
    fun cleanup() {
        fakeKafkaProducer.close()
    }

    @Test
    fun `skal prosessere upubliserte meldinger og sende til Kafka`() {
        // Gitt: En upublisert melding i outbox
        val kafkaMelding =
            KafkaMelding(
                topic = spilleromBehandlingerTopic,
                key = "test-key-123",
                payload = """{"type": "test", "data": "test-data"}""",
            )
        outboxDaoFake.lagreTilOutbox(kafkaMelding)

        // Når: OutboxService prosesserer meldingene
        val antallProsessert = outboxService.prosesserOutbox()

        // Så: Meldingen skal være sendt til Kafka og markert som publisert
        assertEquals(1, antallProsessert)
        assertEquals(1, getSentMessages().size)

        val sentMessage = getSentMessages().first()
        assertEquals(spilleromBehandlingerTopic, sentMessage.topic)
        assertEquals("test-key-123", sentMessage.key)
        assertEquals("""{"type": "test", "data": "test-data"}""", sentMessage.value)

        // Verifiser at headers er satt
        assertTrue(sentMessage.headers.containsKey("outbox-id"))
        assertTrue(sentMessage.headers.containsKey("outbox-opprettet"))

        // Verifiser at meldingen er markert som publisert
        assertTrue(outboxDaoFake.hentAlleUpubliserteEntries().isEmpty())
    }

    @Test
    fun `skal håndtere flere upubliserte meldinger`() {
        // Gitt: Flere upubliserte meldinger
        outboxDaoFake.lagreTilOutbox(KafkaMelding(spilleromBehandlingerTopic, "key1", """{"test-payload": true}"""))
        outboxDaoFake.lagreTilOutbox(KafkaMelding(spilleromBehandlingerTopic, "key2", """{"test-payload": true}"""))
        outboxDaoFake.lagreTilOutbox(KafkaMelding(spilleromBehandlingerTopic, "key3", """{"test-payload": true}"""))

        // Når: OutboxService prosesserer meldingene
        val antallProsessert = outboxService.prosesserOutbox()

        // Så: Alle meldingene skal være prosessert
        assertEquals(3, antallProsessert)
        assertEquals(3, getSentMessages().size)

        assertTrue(outboxDaoFake.hentAlleUpubliserteEntries().isEmpty())
    }

    @Test
    fun `skal støtte flere forskjellige topic`() {
        outboxDaoFake.lagreTilOutbox(KafkaMelding(spilleromBehandlingerTopic, "key-standard", """{"test-payload": true}"""))
        outboxDaoFake.lagreTilOutbox(KafkaMelding(utbetalingerTopic, "key-annen", """{"test-payload": true}"""))

        val antallProsessert = outboxService.prosesserOutbox()

        assertEquals(2, antallProsessert)
        val sentMessages = getSentMessages()
        assertEquals(listOf(spilleromBehandlingerTopic, utbetalingerTopic), sentMessages.map { it.topic })
        assertEquals(listOf("key-standard", "key-annen"), sentMessages.map { it.key })
    }

    @Test
    fun `skal returnere 0 når ingen upubliserte meldinger finnes`() {
        // Når: OutboxService prosesserer uten meldinger
        val antallProsessert = outboxService.prosesserOutbox()

        // Så: Ingen meldinger skal være prosessert
        assertEquals(0, antallProsessert)
        assertTrue(getSentMessages().isEmpty())
    }

    @Test
    fun `skal håndtere feil ved Kafka-sending uten å markere som publisert`() {
        // Gitt: En upublisert melding og en feilende Kafka producer
        outboxDaoFake.lagreTilOutbox(KafkaMelding(spilleromBehandlingerTopic, "test-key", """{"test-payload": true}"""))

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

        val outboxServiceMedFeil = OutboxService(outboxDao = outboxDaoFake, kafkaProducer = feilendeProducer, låsProvider = låsProvider)

        // Når: OutboxService prosesserer meldingen
        val antallProsessert = outboxServiceMedFeil.prosesserOutbox()

        // Så: Ingen meldinger skal være prosessert og meldingen skal fortsatt være upublisert
        assertEquals(0, antallProsessert)

        val upubliserteMeldinger = outboxDaoFake.hentAlleUpubliserteEntries()
        assertEquals(1, upubliserteMeldinger.size)
        assertEquals("test-key", upubliserteMeldinger.first().kafkaKey)
    }

    @Test
    fun `skal prosessere meldinger i riktig rekkefølge basert på id`() {
        // Gitt: Meldinger lagt til i tilfeldig rekkefølge
        outboxDaoFake.lagreTilOutbox(KafkaMelding(spilleromBehandlingerTopic, "key3", """{"test-payload": true}"""))
        outboxDaoFake.lagreTilOutbox(KafkaMelding(spilleromBehandlingerTopic, "key1", """{"test-payload": true}"""))
        outboxDaoFake.lagreTilOutbox(KafkaMelding(spilleromBehandlingerTopic, "key2", """{"test-payload": true}"""))

        // Når: OutboxService prosesserer meldingene
        outboxService.prosesserOutbox()

        // Så: Meldingene skal være sendt i riktig rekkefølge (basert på id)
        val sentMessages = getSentMessages()
        assertEquals(3, sentMessages.size)

        // Verifiser at meldingene er sendt i riktig rekkefølge
        assertEquals("key3", sentMessages[0].key) // Første melding (id=1)
        assertEquals("key1", sentMessages[1].key) // Andre melding (id=2)
        assertEquals("key2", sentMessages[2].key) // Tredje melding (id=3)
    }

    private fun getSentMessages(): List<SentMessage> {
        val behandlingerMeldinger = fakeKafkaProducer.getSentMessages()[spilleromBehandlingerTopic] ?: emptyList()
        val utbetalingerMeldinger = fakeKafkaProducer.getSentMessages()[utbetalingerTopic] ?: emptyList()
        return (behandlingerMeldinger + utbetalingerMeldinger).toList()
    }
}
