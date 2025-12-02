package no.nav.helse.bakrommet.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class OutboxService(
    private val outboxDao: OutboxDao,
    private val kafkaProducer: KafkaProducerInterface,
) {
    private val logger: Logger = LoggerFactory.getLogger(OutboxService::class.java)

    constructor(dataSource: DataSource, kafkaProducer: KafkaProducerInterface) : this(OutboxDaoPg(dataSource), kafkaProducer)

    fun prosesserOutbox(): Int {
        var antallProsessert = 0

        try {
            logger.debug("Starter prosessering av outbox")

            val upubliserteMeldinger = outboxDao.hentAlleUpubliserteEntries()

            if (upubliserteMeldinger.isEmpty()) {
                logger.debug("Ingen upubliserte meldinger funnet i outbox")
                return 0
            }

            logger.info("Fant ${upubliserteMeldinger.size} upubliserte meldinger i outbox")

            upubliserteMeldinger.forEach { entry ->
                try {
                    logger.debug("Prosesserer melding med id=${entry.id}, key=${entry.kafkaKey}")

                    kafkaProducer
                        .send(
                            topic = entry.topic,
                            key = entry.kafkaKey,
                            value = entry.kafkaPayload,
                            headers =
                                mapOf(
                                    "outbox-id" to entry.id.toString(),
                                    "outbox-opprettet" to entry.opprettet.toString(),
                                ),
                        ).get() // Blokkerer til meldingen er sendt

                    outboxDao.markerSomPublisert(entry.id)
                    logger.debug("Melding med id=${entry.id} sendt til Kafka og markert som publisert")
                    antallProsessert++
                } catch (e: Exception) {
                    logger.error("Feil ved sending av melding med id=${entry.id} til Kafka", e)
                    throw e
                }
            }

            logger.info(
                "Fullførte prosessering av outbox, prosesserte $antallProsessert meldinger",
            )

            return antallProsessert
        } catch (e: Exception) {
            logger.error("Feil ved prosessering av outbox", e)
            return antallProsessert
        }
    }
}
