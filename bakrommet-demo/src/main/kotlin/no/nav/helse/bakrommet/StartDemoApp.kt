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
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.Rolle
import no.nav.helse.bakrommet.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.fakedaos.*
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.mockclients.skapClienter
import no.nav.helse.bakrommet.scenarioer.alleTestdata
import no.nav.helse.bakrommet.scenarioer.opprettTestdata
import no.nav.helse.bakrommet.util.objectMapper
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
                            navn = "Saks McBehandlersen",
                            navIdent = "Z123456",
                            preferredUsername = "saks.mcbehandlersen@nav.no",
                            roller = setOf(Rolle.SAKSBEHANDLER, Rolle.LES),
                        ),
                    )
                }
            }
        }

        install(CallLogging) {
            disableDefaultColors()
            logger = appLogger
            level = Level.INFO
            filter { call -> call.request.path().let { it != "/isalive" && it != "/isready" } }
        }

        val testpersoner = alleTestdata()
        val clienter = skapClienter(testpersoner)
        val services = createServices(clienter, DbDaoerFake())

        intercept(ApplicationCallPipeline.Plugins) {
            val cookieNavn = "user_session"
            // spillerom sender cookien tilbake i Authorization-header fordi vi ikke proxyer cookies
            val userSession = call.request.authorization()?.replace("Bearer ", "")

            val faktiskSesjon =
                if (userSession == null || !sessionsDaoer.containsKey(userSession)) {
                    UUID.randomUUID().toString().also {
                        val sessionid = userSession ?: it
                        sessionsDaoer[sessionid] = FakeDaoer()
                        val ctx = CoroutineSessionContext(sessionid)
                        withContext(ctx) {
                            services.opprettTestdata(testpersoner)
                        }
                        // TODO her kan vi sette opp testdata per session som skal i databasene. Helst via services?
                        val maxAge = 4 * 60 * 60 // 4 timer i sekunder
                        call.response.headers.append(
                            "set-cookie",
                            "$cookieNavn=$sessionid; Path=/; HttpOnly; Max-Age=$maxAge",
                        )
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

        routing {
            authenticate("manual") {
                setupRoutes(services, clienter)
            }
        }

        appLogger.info("Starter bakrommet")
    }.start(true)
}
