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
import no.nav.helse.bakrommet.behandling.BehandlingService
import no.nav.helse.bakrommet.behandling.behandlingRoute
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.behandling.dokumenter.dokumenterRoute
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.InntektsmeldingMatcherService
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagService
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.sykepengegrunnlagRoute
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektService
import no.nav.helse.bakrommet.behandling.tilkommen.tilkommenInntektRoute
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningService
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregningRoute
import no.nav.helse.bakrommet.behandling.vilkaar.VilkårService
import no.nav.helse.bakrommet.behandling.vilkaar.saksbehandlingsperiodeVilkårRoute
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.saksbehandlingsperiodeYrkesaktivitetRoute
import no.nav.helse.bakrommet.bruker.brukerRoute
import no.nav.helse.bakrommet.ereg.EregClient
import no.nav.helse.bakrommet.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.infrastruktur.db.DaoerFelles
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoerImpl
import no.nav.helse.bakrommet.infrastruktur.db.SessionDaoerFelles
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactoryPg
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.kafka.KafkaProducerImpl
import no.nav.helse.bakrommet.kafka.OutboxService
import no.nav.helse.bakrommet.organisasjon.OrganisasjonService
import no.nav.helse.bakrommet.organisasjon.organisasjonRoute
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.person.PersonIdService
import no.nav.helse.bakrommet.person.PersonsøkService
import no.nav.helse.bakrommet.person.personinfoRoute
import no.nav.helse.bakrommet.person.personsøkRoute
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.sykepengesoknad.soknaderRoute
import no.nav.helse.bakrommet.tidslinje.TidslinjeService
import no.nav.helse.bakrommet.tidslinje.tidslinjeRoute
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

fun skapDbDaoer(dataSource: DataSource) =
    DbDaoerImpl(
        DaoerFelles(dataSource),
        TransactionalSessionFactoryPg(dataSource) { session ->
            SessionDaoerFelles(session)
        },
    )

fun Application.settOppKtor(
    dataSource: DataSource,
    configuration: Configuration,
    clienter: Clienter = createClients(configuration),
    services: Services =
        createServices(
            clienter,
            skapDbDaoer(dataSource),
        ),
) {
    azureAdAppAuthentication(configuration.auth, configuration.roller)
    helsesjekker()

    appModul(configuration, services, clienter)
}

fun Application.helsesjekker() {
    routing {
        get("/isready") {
            call.respondText("I'm ready")
        }
        get("/isalive") {
            call.respondText("I'm alive")
        }
    }
}

fun Route.setupRoutes(
    services: Services,
    clienter: Clienter,
) {
    personsøkRoute(services.personsøkService)
    personinfoRoute(clienter.pdlClient, services.personIdService)
    soknaderRoute(clienter.sykepengesoknadBackendClient, services.personIdService)
    sykepengegrunnlagRoute(services.sykepengegrunnlagService)
    behandlingRoute(service = services.behandlingService)
    dokumenterRoute(dokumentHenter = services.dokumentHenter)
    saksbehandlingsperiodeVilkårRoute(service = services.vilkårService)
    saksbehandlingsperiodeYrkesaktivitetRoute(
        yrkesaktivitetService = services.yrkesaktivitetService,
        inntektservice = services.inntektService,
        inntektsmeldingMatcherService = services.inntektsmeldingMatcherService,
        personIdService = services.personIdService,
    )
    beregningRoute(service = services.utbetalingsberegningService)
    brukerRoute()
    organisasjonRoute(services.organisasjonService)
    tilkommenInntektRoute(services.tilkommenInntektService)
    tidslinjeRoute(services.tidslinjeService)
}

class Clienter(
    val pdlClient: PdlClient,
    val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    val aInntektClient: AInntektClient,
    val aaRegClient: AARegClient,
    val eregClient: EregClient,
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
    val eregClient = EregClient(configuration.ereg)
    val inntektsmeldingClient = InntektsmeldingClient(configuration.inntektsmelding, oboClient)
    val sigrunClient = SigrunClient(configuration.sigrun, oboClient)

    return Clienter(
        pdlClient = pdlClient,
        sykepengesoknadBackendClient = sykepengesoknadBackendClient,
        aInntektClient = aInntektClient,
        aaRegClient = aaRegClient,
        eregClient = eregClient,
        inntektsmeldingClient = inntektsmeldingClient,
        sigrunClient = sigrunClient,
    )
}

data class Services(
    val personsøkService: PersonsøkService,
    val sykepengegrunnlagService: SykepengegrunnlagService,
    val behandlingService: BehandlingService,
    val dokumentHenter: DokumentHenter,
    val vilkårService: VilkårService,
    val yrkesaktivitetService: YrkesaktivitetService,
    val inntektService: InntektService,
    val inntektsmeldingMatcherService: InntektsmeldingMatcherService,
    val utbetalingsberegningService: UtbetalingsberegningService,
    val personIdService: PersonIdService,
    val organisasjonService: OrganisasjonService,
    val tilkommenInntektService: TilkommenInntektService,
    val tidslinjeService: TidslinjeService,
)

fun createServices(
    clienter: Clienter,
    db: DbDaoer<AlleDaoer>,
): Services {
    val dokumentHenter =
        DokumentHenter(
            db = db,
            soknadClient = clienter.sykepengesoknadBackendClient,
            aInntektClient = clienter.aInntektClient,
            aaRegClient = clienter.aaRegClient,
            sigrunClient = clienter.sigrunClient,
        )
    return Services(
        personsøkService =
            PersonsøkService(
                pdlClient = clienter.pdlClient,
                db = db,
            ),
        sykepengegrunnlagService = SykepengegrunnlagService(db),
        behandlingService =
            BehandlingService(
                db = db,
                dokumentHenter = dokumentHenter,
            ),
        dokumentHenter = dokumentHenter,
        vilkårService = VilkårService(db),
        yrkesaktivitetService = YrkesaktivitetService(db),
        inntektService =
            InntektService(
                db,
                clienter.inntektsmeldingClient,
                clienter.sigrunClient,
                clienter.aInntektClient,
            ),
        inntektsmeldingMatcherService =
            InntektsmeldingMatcherService(
                db = db,
                clienter.inntektsmeldingClient,
            ),
        utbetalingsberegningService = UtbetalingsberegningService(db),
        personIdService = PersonIdService(db),
        organisasjonService = OrganisasjonService(clienter.eregClient),
        tilkommenInntektService = TilkommenInntektService(db),
        tidslinjeService = TidslinjeService(db, clienter.eregClient),
    )
}

internal fun Application.appModul(
    configuration: Configuration,
    services: Services,
    clienter: Clienter,
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

    installErrorHandling(includeStackTrace = configuration.naisClusterName == "dev-gcp")

    routing {
        authenticate("entraid") {
            install(RolleMatrise)
            setupRoutes(services, clienter)
        }
    }
}
