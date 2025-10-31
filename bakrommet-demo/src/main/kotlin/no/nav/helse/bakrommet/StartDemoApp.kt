package no.nav.helse.bakrommet

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.fakedaos.*
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.SessionDaoerFelles
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.util.sikkerLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

fun Application.requestContextPlugin() {
/*
    @Serializable
    data class DemoSession(val userId: String?)
    install(Sessions) {
        cookie<DemoSession>("DEMO_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 60 * 60 * 24 // 24 timer
            // Bruker enkel cookie uten transform for demo
        }
    }
*/
    intercept(ApplicationCallPipeline.Plugins) {
        call.response.header("dsf", "heihei")
        val ctx = CoroutineSessionContext(sessionid = "test-string")
        withContext(ctx) {
            proceed()
        }
    }
}

object FakeDaoer : AlleDaoer {
    override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDaoFake()
    override val saksbehandlingsperiodeEndringerDao = SaksbehandlingsperiodeEndringerDaoFake()
    override val personDao = PersonDaoFake()
    override val dokumentDao = DokumentDaoFake()
    override val yrkesaktivitetDao = YrkesaktivitetDaoFake()
    override val vurdertVilkårDao = VurdertVilkårDaoFake()
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoFake()
    override val beregningDao = UtbetalingsberegningDaoFake()
    override val outboxDao = OutboxDaoFake()
}

object FakeTransactionlfactory : TransactionalSessionFactory<SessionDaoerFelles> {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <RET> transactionalSessionScope(transactionalBlock: suspend (SessionDaoerFelles) -> RET): RET {
        // Demoen bruker kun fake-DAOer og trenger ingen ekte transaksjon/session.
        // Vi kaster funksjonen til å akseptere AlleDaoer, og kjører den med FakeDaoer.
        val block = transactionalBlock as (AlleDaoer) -> RET
        return block(FakeDaoer)
    }
}

suspend fun doSomething(prefix: String) {
    println("2: ${currentCoroutineContext()[CoroutineSessionContext.Key]?.sessionid}")
}

fun main() {
    embeddedServer(CIO, port = 8080) {
        appLogger.info("Setter opp ktor")

        helsesjekker()

        requestContextPlugin()
        routing {
            get("/") {
                println("1: ${coroutineContext[CoroutineSessionContext.Key]?.sessionid}")
                println("2: ${currentCoroutineContext()[CoroutineSessionContext.Key]?.sessionid}")

                doSomething("sdfdf")
                call.respondText("Hello World")
            }
        }
        install(CallLogging) {
            disableDefaultColors()
            logger = sikkerLogger
            level = Level.INFO
            filter { call -> call.request.path().let { it != "/isalive" && it != "/isready" } }
        }

        installErrorHandling(true)

        // Sett opp mock-klienter tilsvarende e2e-testene
        val oboClient: OboClient = PdlMock.createDefaultOboClient()

        val pdlClient = PdlMock.pdlClient(oboClient = oboClient)
        val aaRegClient = AARegMock.aaRegClientMock(oboClient = oboClient)
        val aInntektClient = AInntektMock.aInntektClientMock(oboClient = oboClient)
        val sigrunClient = SigrunMock.sigrunMockClient(oboClient = oboClient)
        val inntektsmeldingClient = InntektsmeldingApiMock.inntektsmeldingClientMock(oboClient = oboClient)

        // Enkel mock av sykepengesøknad-backend-klienten for demoformål
        val sykepengesoknadBackendClient =
            SykepengesoknadBackendClient(
                configuration =
                    Configuration.SykepengesoknadBackend(
                        hostname = "sykepengesoknad-backend",
                        scope = OAuthScope("sykepengesoknad-backend-scope"),
                    ),
                oboClient = oboClient,
            )

        val clienter =
            Clienter(
                pdlClient = pdlClient,
                sykepengesoknadBackendClient = sykepengesoknadBackendClient,
                oboClient = oboClient,
                aInntektClient = aInntektClient,
                aaRegClient = aaRegClient,
                inntektsmeldingClient = inntektsmeldingClient,
                sigrunClient = sigrunClient,
            )
        val services = createServices(clienter, FakeDaoer, FakeTransactionlfactory)
        routing {
            setupRoutes(services)
        }

        appLogger.info("Starter bakrommet")
    }.start(true)
}
