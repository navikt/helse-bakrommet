package no.nav.helse.bakrommet.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class OutboxService(private val outboxDao: OutboxDao) {
    private val logger: Logger = LoggerFactory.getLogger(OutboxService::class.java)

    constructor(dataSource: DataSource) : this(OutboxDao(dataSource))

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
                // TODO: Her vil vi senere implementere selve kafka-produseringen
                logger.debug("Prosesserer melding med id=${entry.id}, key=${entry.kafkaKey}")

                outboxDao.markerSomPublisert(entry.id)
                logger.debug("Melding med id=${entry.id} markert som publisert")
                antallProsessert++
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
