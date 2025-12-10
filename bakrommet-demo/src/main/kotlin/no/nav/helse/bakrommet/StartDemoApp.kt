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
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.sessions
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.withContext
import no.nav.helse.bakrommet.api.setupApiRoutes
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.fakedaos.*
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.mockclients.skapClienter
import no.nav.helse.bakrommet.testdata.alleTestdata
import no.nav.helse.bakrommet.testdata.opprettTestdata
import no.nav.helse.bakrommet.util.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.UUID

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

class FakeDaoer : AlleDaoer {
    override val behandlingDao = BehandlingDaoFake()
    override val behandlingEndringerDao = BehandlingEndringerDaoFake()
    override val personPseudoIdDao = PersonPseudoIdDaoFake()
    override val dokumentDao = DokumentDaoFake()
    override val yrkesaktivitetDao = YrkesaktivitetDaoFake()
    override val vurdertVilkårDao = VurdertVilkårDaoFake()
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoFake()
    override val beregningDao = UtbetalingsberegningDaoFake()
    override val outboxDao = OutboxDaoFake()
    override val tilkommenInntektDao = TilkommenInntektDaoFake()
}

val sessionsDaoer = mutableMapOf<String, FakeDaoer>()
val sessionsBrukere = mutableMapOf<String, Bruker>()
private val helsesjekkPaths = setOf("/isalive", "/isready")

private fun ApplicationCall.erHelsesjekk() = request.path() in helsesjekkPaths

private fun ApplicationCall.erApiKall() = request.path().startsWith("/v1") || request.path().startsWith("/v2") || request.path().startsWith("/v2")

class DbDaoerFake : DbDaoer<AlleDaoer> {
    private suspend fun hentSessionDaoer(): AlleDaoer = sessionsDaoer[hentSession()] ?: throw IllegalStateException("Ingen Daoer funnet for session")

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
                    val sessionIdFraCookie = ctx.call.sessions.get("bakrommet-demo-session") as String?
                    val bruker =
                        if (sessionIdFraCookie != null) {
                            sessionsBrukere[sessionIdFraCookie] ?: predefinerteBrukere.first()
                        } else {
                            predefinerteBrukere.first()
                        }
                    ctx.principal(bruker)
                }
            }
        }

        install(CallLogging) {
            disableDefaultColors()
            logger = appLogger
            level = Level.INFO
            filter { call -> !call.erHelsesjekk() }
        }

        val clienter = skapClienter(alleTestdata)
        val services = createServices(clienter, DbDaoerFake())

        install(Sessions) {
            cookie<String>("bakrommet-demo-session", SessionStorageMemory()) {
                cookie.path = "/"
                cookie.maxAgeInSeconds = 60 * 60 * 4 // 4 timer
            }
        }

        intercept(ApplicationCallPipeline.Plugins) {
            if (!call.erApiKall()) {
                // Starter ikke sesjoner for ikke-API kall
                proceed()
                return@intercept
            }

            val sessionIdFraCookie = call.sessions.get("bakrommet-demo-session") as String?

            val sessionId =
                if (sessionIdFraCookie == null || !sessionsDaoer.containsKey(sessionIdFraCookie)) {
                    UUID.randomUUID().toString().also {
                        val sessionid = sessionIdFraCookie ?: it
                        sessionsDaoer[sessionid] = FakeDaoer()
                        if (!sessionsBrukere.containsKey(sessionid)) {
                            sessionsBrukere[sessionid] = predefinerteBrukere.first()
                        }
                        val ctx = CoroutineSessionContext(sessionid)
                        withContext(ctx) {
                            services.opprettTestdata(alleTestdata)
                        }
                        call.sessions.set("bakrommet-demo-session", sessionid)
                    }
                } else {
                    sessionIdFraCookie
                }
            val ctx =
                CoroutineSessionContext(
                    sessionId,
                )

            withContext(ctx) {
                proceed()
            }
        }

        intercept(ApplicationCallPipeline.Plugins) {
            call.request.setHeader("Authorization", listOf("Bearer DEMO-TOKEN"))
        }

        installErrorHandling(true)

        routing {
            demoBrukerRoute()
            demoTestdataRoute()
            demoOutboxRoute()
            authenticate("manual") {
                setupApiRoutes(services)
            }
        }

        appLogger.info("Starter bakrommet")
    }.start(true)
}
