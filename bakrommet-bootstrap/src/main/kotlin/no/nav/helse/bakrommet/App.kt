package no.nav.helse.bakrommet

import io.ktor.server.application.ApplicationStarted
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.helse.bakrommet.api.settOppKtor
import no.nav.helse.bakrommet.db.dao.OutboxDaoPg
import no.nav.helse.bakrommet.db.instansierDatabase
import no.nav.helse.bakrommet.db.skapDbDaoer
import no.nav.helse.bakrommet.kafka.KafkaProducerImpl
import no.nav.helse.bakrommet.kafka.OutboxService

fun main() {
    val configuration = Configuration.fromEnv()
    val dataSource = instansierDatabase(configuration.db)
    val clienter: Clienter = createClients(configuration)
    val services: Services = createServices(clienter, skapDbDaoer(dataSource))

    embeddedServer(CIO, port = 8080) {
        appLogger.info("Setter opp ktor")
        settOppKtor(
            authOgRollerConfig = configuration,
            services = services,
            errorHandlingIncludeStackTrace = configuration.naisClusterName == "dev-gcp",
        )
        appLogger.info("Starter bakrommet")
        monitor.subscribe(ApplicationStarted) {
            val kafkaProducer = KafkaProducerImpl()
            val outboxService = OutboxService(OutboxDaoPg(dataSource), dataSource, kafkaProducer)
            launch {
                while (true) {
                    outboxService.prosesserOutbox()
                    delay(30_000)
                }
            }
        }
        monitor.subscribe(ApplicationStarted) {
            launch {
                while (true) {
                    val antallSlettet: Int = services.personService.slettPseudoIderEldreEnn(antallDager = 7)
                    appLogger.info("Slettet {} utgåtte pseudoIder", antallSlettet)
                    delay(3600_000)
                }
            }
        }
    }.start(true)
}
