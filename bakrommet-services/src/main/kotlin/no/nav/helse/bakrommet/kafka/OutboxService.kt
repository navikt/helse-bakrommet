package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.`LåsProvider`
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

private val LOCK_AT_MOST_FOR = Duration.ofHours(1)
private val LOCK_AT_LEAST_FOR = Duration.ofSeconds(10)

class OutboxService(
    private val outboxDao: OutboxDao,
    private val kafkaProducer: MeldingProducer,
    private val låsProvider: LåsProvider,
) {
    private val logger: Logger = LoggerFactory.getLogger(OutboxService::class.java)

    fun prosesserOutbox(): Int {
        val resultat =
            låsProvider.kjørMedLås(LOCK_AT_LEAST_FOR, LOCK_AT_MOST_FOR) {
                doProsesserOutbox()
            }
        return resultat
    }

    private fun doProsesserOutbox(): Int {
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
