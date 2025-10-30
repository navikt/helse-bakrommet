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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import no.nav.helse.bakrommet.kafka.KafkaProducerImpl
import no.nav.helse.bakrommet.kafka.OutboxService
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.PersonsøkService
import no.nav.helse.bakrommet.person.personinfoRoute
import no.nav.helse.bakrommet.person.personsøkRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeService
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.dokumenterRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektService
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektsmeldingMatcherService
import no.nav.helse.bakrommet.saksbehandlingsperiode.saksbehandlingsperiodeRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagService
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.sykepengegrunnlagRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDaoPg
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningService
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregningRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.demoUtbetalingsberegningRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VilkårService
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.saksbehandlingsperiodeVilkårRoute
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.saksbehandlingsperiodeYrkesaktivitetRoute
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

fun startApp(configuration: Configuration) {
    appLogger.info("Setter opp data source")
    val dataSource = instansierDatabase(configuration.db)

    embeddedServer(CIO, port = 8080) {
        appLogger.info("Setter opp ktor")
        settOppKtor(dataSource, configuration)
        appLogger.info("Starter bakrommet")
        monitor.subscribe(ApplicationStarted) {
            val kafkaProducer = KafkaProducerImpl()
            val outboxService = OutboxService(dataSource, kafkaProducer)
            launch {
                while (true) {
                    outboxService.prosesserOutbox()
                    delay(30_000)
                }
            }
        }
    }.start(true)
}

internal fun instansierDatabase(configuration: Configuration.DB) = DBModule(configuration = configuration).also { it.migrate() }.dataSource

fun Application.settOppKtor(
    dataSource: DataSource,
    configuration: Configuration,
    clienter: Clienter = createClients(configuration),
    services: Services = createServices(dataSource, clienter),
) {
    azureAdAppAuthentication(configuration.auth, configuration.roller)
    helsesjekker()

    appModul(configuration, services)
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

fun Route.setupRoutes(services: Services) {
    personsøkRoute(services.personsøkService)
    personinfoRoute(services.personService.pdlClient, services.personService.personDao)
    soknaderRoute(services.søknaderService.soknadClient, services.søknaderService.personDao)
    sykepengegrunnlagRoute(services.sykepengegrunnlagService)
    saksbehandlingsperiodeRoute(service = services.saksbehandlingsperiodeService)
    dokumenterRoute(dokumentHenter = services.dokumentHenter)
    saksbehandlingsperiodeVilkårRoute(service = services.vilkårService)
    saksbehandlingsperiodeYrkesaktivitetRoute(
        service = services.yrkesaktivitetService,
        inntektservice = services.inntektService,
        inntektsmeldingMatcherService = services.inntektsmeldingMatcherService,
    )
    beregningRoute(service = services.utbetalingsberegningService)
    brukerRoute()
}

class Clienter(
    val pdlClient: PdlClient,
    val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    val oboClient: OboClient,
    val aInntektClient: AInntektClient,
    val aaRegClient: AARegClient,
    val inntektsmeldingClient: InntektsmeldingClient,
    val sigrunClient: SigrunClient,
)

fun createClients(configuration: Configuration): Clienter {
    val oboClient = OboClient(configuration.obo)
    val pdlClient = PdlClient(configuration.pdl, oboClient)
    val sykepengesoknadBackendClient =
        SykepengesoknadBackendClient(
            configuration.sykepengesoknadBackend,
            oboClient,
        )

    val aaRegClient = AARegClient(configuration.aareg, oboClient)
    val aInntektClient = AInntektClient(configuration.ainntekt, oboClient)
    val inntektsmeldingClient = InntektsmeldingClient(configuration.inntektsmelding, oboClient)
    val sigrunClient = SigrunClient(configuration.sigrun, oboClient)

    return Clienter(
        pdlClient = pdlClient,
        sykepengesoknadBackendClient = sykepengesoknadBackendClient,
        oboClient = oboClient,
        aInntektClient = aInntektClient,
        aaRegClient = aaRegClient,
        inntektsmeldingClient = inntektsmeldingClient,
        sigrunClient = sigrunClient,
    )
}

data class Services(
    val personsøkService: PersonsøkService,
    val personService: PersonService,
    val søknaderService: SøknaderService,
    val sykepengegrunnlagService: SykepengegrunnlagService,
    val saksbehandlingsperiodeService: SaksbehandlingsperiodeService,
    val dokumentHenter: DokumentHenter,
    val vilkårService: VilkårService,
    val yrkesaktivitetService: YrkesaktivitetService,
    val inntektService: InntektService,
    val inntektsmeldingMatcherService: InntektsmeldingMatcherService,
    val utbetalingsberegningService: UtbetalingsberegningService,
)

data class PersonService(
    val pdlClient: PdlClient,
    val personDao: PersonDao,
)

data class SøknaderService(
    val soknadClient: SykepengesoknadBackendClient,
    val personDao: PersonDao,
)

fun createServices(
    dataSource: DataSource,
    clienter: Clienter,
    daoerFelles: DaoerFelles = DaoerFelles(dataSource),
): Services {
    val sessionFactoryFelles: TransactionalSessionFactory<SessionDaoerFelles> =
        TransactionalSessionFactory(dataSource) { session ->
            SessionDaoerFelles(session)
        }

    val personsøkService =
        PersonsøkService(
            pdlClient = clienter.pdlClient,
            personDao = daoerFelles.personDao,
        )

    val personService =
        PersonService(
            pdlClient = clienter.pdlClient,
            personDao = daoerFelles.personDao,
        )

    val søknaderService =
        SøknaderService(
            soknadClient = clienter.sykepengesoknadBackendClient,
            personDao = daoerFelles.personDao,
        )

    val dokumentHenter =
        DokumentHenter(
            personDao = daoerFelles.personDao,
            saksbehandlingsperiodeDao = daoerFelles.saksbehandlingsperiodeDao,
            dokumentDao = daoerFelles.dokumentDao,
            soknadClient = clienter.sykepengesoknadBackendClient,
            aInntektClient = clienter.aInntektClient,
            aaRegClient = clienter.aaRegClient,
            sigrunClient = clienter.sigrunClient,
        )

    val saksbehandlingsperiodeService =
        SaksbehandlingsperiodeService(
            daoer = daoerFelles,
            sessionFactory = sessionFactoryFelles,
            dokumentHenter = dokumentHenter,
        )

    val sykepengegrunnlagService =
        SykepengegrunnlagService(
            daoer = daoerFelles,
            sessionFactory = sessionFactoryFelles,
        )

    val vilkårService = VilkårService(daoerFelles, sessionFactoryFelles)

    val yrkesaktivitetService =
        YrkesaktivitetService(
            daoerFelles,
            sessionFactoryFelles,
        )

    val inntektService =
        InntektService(
            daoerFelles,
            clienter.inntektsmeldingClient,
            clienter.sigrunClient,
            clienter.aInntektClient,
            sessionFactoryFelles,
        )

    val inntektsmeldingMatcherService =
        InntektsmeldingMatcherService(
            daoerFelles,
            sessionFactoryFelles,
            clienter.inntektsmeldingClient,
        )

    val utbetalingsberegningService =
        UtbetalingsberegningService(
            UtbetalingsberegningDaoPg(dataSource),
        )

    return Services(
        personsøkService = personsøkService,
        personService = personService,
        søknaderService = søknaderService,
        sykepengegrunnlagService = sykepengegrunnlagService,
        saksbehandlingsperiodeService = saksbehandlingsperiodeService,
        dokumentHenter = dokumentHenter,
        vilkårService = vilkårService,
        yrkesaktivitetService = yrkesaktivitetService,
        inntektService = inntektService,
        inntektsmeldingMatcherService = inntektsmeldingMatcherService,
        utbetalingsberegningService = utbetalingsberegningService,
    )
}

internal fun Application.appModul(
    configuration: Configuration,
    services: Services,
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
        // Demo API - åpen endpoint uten autentisering
        demoUtbetalingsberegningRoute()

        authenticate("entraid") {
            install(RolleMatrise)
            setupRoutes(services)
        }
    }
}
