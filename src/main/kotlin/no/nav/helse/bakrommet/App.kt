package no.nav.helse.bakrommet

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.RolleMatrise
import no.nav.helse.bakrommet.auth.azureAdAppAuthentication
import no.nav.helse.bakrommet.bruker.brukerRoute
import no.nav.helse.bakrommet.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.infrastruktur.db.DaoerFelles
import no.nav.helse.bakrommet.infrastruktur.db.SessionDaoerFelles
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.inntektsmelding.inntektsmeldingerRoute
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.personinfoRoute
import no.nav.helse.bakrommet.person.personsøkRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeService
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.dokumenterRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdService
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.saksbehandlingsperiodeInntektsforholdRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.saksbehandlingsperiodeRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VilkårService
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.saksbehandlingsperiodeVilkårRoute
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.sykepengesoknad.soknaderRoute
import no.nav.helse.bakrommet.util.objectMapper
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
    oboClient: OboClient = OboClient(configuration.obo),
    pdlClient: PdlClient = PdlClient(configuration.pdl, oboClient),
    sykepengesoknadBackendClient: SykepengesoknadBackendClient =
        SykepengesoknadBackendClient(
            configuration.sykepengesoknadBackend,
            oboClient,
        ),
    aaRegClient: AARegClient = AARegClient(configuration.aareg, oboClient),
    aInntektClient: AInntektClient = AInntektClient(configuration.ainntekt, oboClient),
    inntektsmeldingClient: InntektsmeldingClient = InntektsmeldingClient(configuration.inntektsmelding, oboClient),
    sigrunClient: SigrunClient = SigrunClient(configuration.sigrun, oboClient),
) {
    azureAdAppAuthentication(configuration.auth, configuration.roller)
    helsesjekker()
    appModul(
        dataSource,
        pdlClient,
        configuration,
        sykepengesoknadBackendClient,
        aaRegClient,
        aInntektClient,
        inntektsmeldingClient,
        sigrunClient,
    )
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
    pdlClient: PdlClient,
    configuration: Configuration,
    sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    aaRegClient: AARegClient,
    aInntektClient: AInntektClient,
    inntektsmeldingClient: InntektsmeldingClient,
    sigrunClient: SigrunClient,
    personDao: PersonDao = PersonDao(dataSource),
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao = SaksbehandlingsperiodeDao(dataSource),
    dokumentDao: DokumentDao = DokumentDao(dataSource),
    daoerFelles: DaoerFelles = DaoerFelles(dataSource),
    sessionFactoryFelles: TransactionalSessionFactory<SessionDaoerFelles> =
        TransactionalSessionFactory(dataSource) { session ->
            SessionDaoerFelles(session)
        },
    dokumentHenter: DokumentHenter =
        DokumentHenter(
            personDao = personDao,
            saksbehandlingsperiodeDao = saksbehandlingsperiodeDao,
            dokumentDao = dokumentDao,
            soknadClient = sykepengesoknadBackendClient,
            aInntektClient = aInntektClient,
            aaRegClient = aaRegClient,
            sigrunClient = sigrunClient,
        ),
    saksbehandlingsperiodeService: SaksbehandlingsperiodeService =
        SaksbehandlingsperiodeService(
            daoer = daoerFelles,
            sessionFactory = sessionFactoryFelles,
            dokumentHenter = dokumentHenter,
        ),
) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
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
            install(RolleMatrise)
            personsøkRoute(pdlClient, personDao)
            personinfoRoute(pdlClient, personDao)
            soknaderRoute(sykepengesoknadBackendClient, personDao)
            saksbehandlingsperiodeRoute(service = saksbehandlingsperiodeService)
            dokumenterRoute(dokumentHenter = dokumentHenter)
            saksbehandlingsperiodeVilkårRoute(
                service = VilkårService(daoerFelles, sessionFactoryFelles),
            )
            inntektsmeldingerRoute(inntektsmeldingClient, personDao)
            saksbehandlingsperiodeInntektsforholdRoute(
                service =
                    InntektsforholdService(
                        daoerFelles,
                        sessionFactoryFelles,
                    ),
            )
            brukerRoute()
        }
    }
}
