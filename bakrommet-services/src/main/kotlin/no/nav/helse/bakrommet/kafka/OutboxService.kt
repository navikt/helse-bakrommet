package no.nav.helse.bakrommet.kafka

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

private val LOCK_AT_MOST_FOR = Duration.ofHours(1)
private val LOCK_AT_LEAST_FOR = Duration.ofSeconds(10)

class OutboxService(
    private val outboxDao: OutboxDao,
    private val kafkaProducer: KafkaProducerInterface,
    private val lockAtMostFor: Duration = LOCK_AT_MOST_FOR,
    private val lockAtLeastFor: Duration = LOCK_AT_LEAST_FOR,
    private val lockingDataSource: DataSource?,
) {
    private val logger: Logger = LoggerFactory.getLogger(OutboxService::class.java)

    constructor(
        dataSource: DataSource,
        kafkaProducer: KafkaProducerInterface,
        lockAtMostFor: Duration = LOCK_AT_MOST_FOR,
        lockAtLeastFor: Duration = LOCK_AT_LEAST_FOR,
    ) : this(OutboxDaoPg(dataSource), kafkaProducer, lockAtMostFor, lockAtLeastFor, dataSource)

    fun prosesserOutbox(kjørMedLås: Boolean = true): Int {
        if (kjørMedLås && (lockingDataSource != null)) {
            logger.debug("Prosesserer Outbox med mindre det finnes en lås")
            val lockProvider = JdbcLockProvider(lockingDataSource, "shedlock")
            val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)
            val task =
                LockingTaskExecutor.TaskWithResult<Int> {
                    try {
                        doProsesserOutbox()
                    } catch (e: Exception) {
                        logger.error("feilUnderKjøring av doProsesserOutbox", e)
                        -1
                    }
                }
            val taskResult =
                lockingExecutor.executeWithLock(
                    task,
                    LockConfiguration(Instant.now(), "outbox-lock", lockAtMostFor, lockAtLeastFor),
                )
            logger.debug("prosesserOutbox m/lås ble kjørt? : {}", taskResult.wasExecuted())
            return taskResult.result ?: 0
        } else {
            if (kjørMedLås) {
                logger.warn("prosesserOutbox kan ikke kjøre med lås for det er ingen dataSource angitt") // Demo/FakeDAO
            }
            return doProsesserOutbox()
        }
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
