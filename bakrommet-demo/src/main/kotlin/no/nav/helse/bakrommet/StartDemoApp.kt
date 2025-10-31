package no.nav.helse.bakrommet

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.*
import io.ktor.server.routing.routing
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.withContext
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.Rolle
import no.nav.helse.bakrommet.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.fakedaos.*
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.sikkerLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.UUID

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

class FakeDaoer : AlleDaoer {
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

val sessionsDaoer = mutableMapOf<String, FakeDaoer>()

class DbDaoerFake : DbDaoer<AlleDaoer> {
    private suspend fun hentSessionDaoer(): AlleDaoer = sessionsDaoer.get(hentSession()) ?: throw IllegalStateException("Ingen Daoer funnet for session")

    override suspend fun <RET> nonTransactional(block: suspend (AlleDaoer.() -> RET)): RET = block(hentSessionDaoer())

    override suspend fun <RET> transactional(block: suspend (AlleDaoer.() -> RET)): RET = block(hentSessionDaoer())
}

@OptIn(InternalAPI::class)
fun main() {
    embeddedServer(CIO, port = 8080) {
        appLogger.info("Setter opp ktor")

        helsesjekker()

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }

        install(Authentication) {
            provider("manual") {
                authenticate { ctx ->
                    // Sett inn din egen principal når du har bestemt at requesten er “innlogget”
                    ctx.principal(
                        Bruker(
                            navn = "Test",
                            navIdent = "a23423",
                            preferredUsername = "sdfsdfs",
                            roller = setOf(Rolle.SAKSBEHANDLER, Rolle.LES),
                        ),
                    )
                }
            }
        }

        install(CallLogging) {
            disableDefaultColors()
            logger = sikkerLogger
            level = Level.INFO
            filter { call -> call.request.path().let { it != "/isalive" && it != "/isready" } }
        }

        intercept(ApplicationCallPipeline.Plugins) {
            val cookieNavn = "user_session"
            // spillerom sender cookien tilbake i Authorization-header fordi vi ikke proxyer cookies
            val userSession = call.request.authorization()?.replace("Bearer ", "")

            val faktiskSesjon =
                if (userSession == null || !sessionsDaoer.containsKey(userSession)) {
                    UUID.randomUUID().toString().also {
                        val sessionid = userSession ?: it
                        sessionsDaoer[sessionid] = FakeDaoer()
                        call.response.headers.append("set-cookie", "$cookieNavn=$sessionid")
                    }
                } else {
                    userSession
                }

            val ctx = CoroutineSessionContext(faktiskSesjon)
            withContext(ctx) {
                proceed()
            }
        }

        intercept(ApplicationCallPipeline.Plugins) {
            call.request.setHeader("Authorization", listOf("Bearer DEMO-TOKEN"))
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
                aInntektClient = aInntektClient,
                aaRegClient = aaRegClient,
                inntektsmeldingClient = inntektsmeldingClient,
                sigrunClient = sigrunClient,
            )
        val services = createServices(clienter, DbDaoerFake())
        routing {
            authenticate("manual") {
                setupRoutes(services, clienter)
            }
        }

        appLogger.info("Starter bakrommet")
    }.start(true)
}
