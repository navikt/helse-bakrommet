package no.nav.helse.bakrommet

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.azureAdAppAuthentication
import no.nav.helse.bakrommet.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.personinfoRoute
import no.nav.helse.bakrommet.person.personsøkRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.saksbehandlingsperiodeRoute
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.sykepengesoknad.soknaderRoute
import no.nav.helse.bakrommet.util.sikkerLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import javax.sql.DataSource

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

fun main() {
    startApp(Configuration.fromEnv())
}

internal fun startApp(configuration: Configuration) {
    appLogger.info("Setter opp data source")
    val dataSource = instansierDatabase(configuration.db)

    embeddedServer(CIO, port = 8080) {
        appLogger.info("Setter opp ktor")
        settOppKtor(dataSource, configuration)
        appLogger.info("Starter bakrommet")
    }.start(true)
}

internal fun instansierDatabase(configuration: Configuration.DB) = DBModule(configuration = configuration).also { it.migrate() }.dataSource

internal fun Application.settOppKtor(
    dataSource: DataSource,
    configuration: Configuration,
    pdlClient: PdlClient = PdlClient(configuration.pdl),
    oboClient: OboClient = OboClient(configuration.obo),
    sykepengesoknadBackendClient: SykepengesoknadBackendClient = SykepengesoknadBackendClient(configuration.sykepengesoknadBackend),
) {
    azureAdAppAuthentication(configuration.auth)
    helsesjekker()
    appModul(dataSource, oboClient, pdlClient, configuration, sykepengesoknadBackendClient)
}

internal fun Application.helsesjekker() {
    routing {
        get("/isready") {
            call.respondText("I'm ready")
        }
        get("/isalive") {
            call.respondText("I'm alive")
        }
    }
}

internal fun Application.appModul(
    dataSource: DataSource,
    oboClient: OboClient,
    pdlClient: PdlClient,
    configuration: Configuration,
    sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    personDao: PersonDao = PersonDao(dataSource),
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao = SaksbehandlingsperiodeDao(dataSource),
) {
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter())
    }

    install(CallLogging) {
        disableDefaultColors()
        logger = sikkerLogger
        level = Level.INFO
        filter { call -> call.request.path().let { it != "/isalive" && it != "/isready" } }
    }

    installErrorHandling(configuration)

    routing {
        authenticate("entraid") {
            personsøkRoute(oboClient, configuration, pdlClient, personDao)
            personinfoRoute(oboClient, configuration, pdlClient, personDao)
            soknaderRoute(oboClient, configuration, sykepengesoknadBackendClient, personDao)
            saksbehandlingsperiodeRoute(saksbehandlingsperiodeDao, personDao)

            get("/v1/{personId}/dokumenter") {
                call.respondText("[]", ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }
}
