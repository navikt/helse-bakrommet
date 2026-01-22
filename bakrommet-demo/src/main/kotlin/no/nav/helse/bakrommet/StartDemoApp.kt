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
import io.ktor.server.sessions.*
import io.ktor.util.collections.ConcurrentSet
import io.ktor.utils.io.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.helse.bakrommet.api.auth.RolleMatrise
import no.nav.helse.bakrommet.api.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.api.setupApiRoutes
import no.nav.helse.bakrommet.db.dao.YrkesaktivitetDaoOverRepository
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetsperiodeId
import no.nav.helse.bakrommet.fakedaos.*
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.mockproviders.skapProviders
import no.nav.helse.bakrommet.repository.BehandlingRepository
import no.nav.helse.bakrommet.repository.TilkommenInntektRepository
import no.nav.helse.bakrommet.repository.VilkårsvurderingRepository
import no.nav.helse.bakrommet.repository.YrkesaktivitetsperiodeRepository
import no.nav.helse.bakrommet.testdata.alleTestdata
import no.nav.helse.bakrommet.testdata.opprettTestdata
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

class FakeDaoer : AlleDaoer {
    override val behandlingRepository: BehandlingRepository =
        object : BehandlingRepository {
            private val behandlinger = ConcurrentHashMap<BehandlingId, Behandling>()

            override fun finn(behandlingId: BehandlingId): Behandling? = behandlinger[behandlingId]

            override fun finnAlle(): List<Behandling> = behandlinger.values.toList()

            override fun finnFor(naturligIdent: NaturligIdent): List<Behandling> = behandlinger.values.filter { it gjelder naturligIdent }

            override fun lagre(behandling: Behandling) {
                behandlinger[behandling.id] = behandling
            }
        }
    override val behandlingDao = BehandlingDaoFake(behandlingRepository)

    override val vilkårsvurderingRepository: VilkårsvurderingRepository =
        object : VilkårsvurderingRepository {
            private val vurderteVilkår = ConcurrentHashMap<VilkårsvurderingId, VurdertVilkår>()

            override fun finn(vilkårsvurderingId: VilkårsvurderingId): VurdertVilkår? = vurderteVilkår[vilkårsvurderingId]

            override fun hentAlle(behandlingId: BehandlingId): List<VurdertVilkår> = vurderteVilkår.values.filter { it.id.behandlingId == behandlingId }

            override fun lagre(vurdertVilkår: VurdertVilkår) {
                vurderteVilkår[vurdertVilkår.id] = vurdertVilkår
            }

            override fun slett(vilkårsvurderingId: VilkårsvurderingId) {
                vurderteVilkår.remove(vilkårsvurderingId)
            }
        }
    override val yrkesaktivitetsperiodeRepository: YrkesaktivitetsperiodeRepository =
        object : YrkesaktivitetsperiodeRepository {
            private val yrkesaktiviteter = ConcurrentHashMap<BehandlingId, MutableList<Yrkesaktivitetsperiode>>()

            override fun finn(behandlingId: BehandlingId): List<Yrkesaktivitetsperiode> = yrkesaktiviteter[behandlingId] ?: emptyList()

            override fun finn(yrkesaktivitetsperiodeId: YrkesaktivitetsperiodeId): Yrkesaktivitetsperiode? = yrkesaktiviteter.values.flatten().find { it.id == yrkesaktivitetsperiodeId }

            override fun lagre(yrkesaktivitetsperiode: Yrkesaktivitetsperiode) {
                val aktiviteterForBehandling = yrkesaktiviteter.getOrPut(yrkesaktivitetsperiode.behandlingId) { mutableListOf() }
                aktiviteterForBehandling.removeIf { it.id == yrkesaktivitetsperiode.id }
                aktiviteterForBehandling.add(yrkesaktivitetsperiode)
            }

            override fun slett(yrkesaktivitetsperiodeId: YrkesaktivitetsperiodeId) {
                yrkesaktiviteter.values.forEach { aktiviteter ->
                    aktiviteter.removeIf { it.id == yrkesaktivitetsperiodeId }
                }
            }
        }
    override val tilkommenInntektRepository =
        object : TilkommenInntektRepository {
            private val tilkomneInntekter = ConcurrentSet<TilkommenInntekt>()

            override fun lagre(tilkommenInntekt: TilkommenInntekt) {
                tilkomneInntekter.removeIf { it.id == tilkommenInntekt.id }
                tilkomneInntekter.add(tilkommenInntekt)
            }

            override fun finn(tilkommenInntektId: TilkommenInntektId): TilkommenInntekt? = tilkomneInntekter.find { it.id == tilkommenInntektId }

            override fun slett(tilkommenInntektId: TilkommenInntektId) {
                tilkomneInntekter.removeIf { it.id == tilkommenInntektId }
            }

            override fun finnFor(behandlingId: BehandlingId): List<TilkommenInntekt> = tilkomneInntekter.filter { it.behandlingId == behandlingId }
        }

    override val behandlingEndringerDao = BehandlingEndringerDaoFake()
    override val personPseudoIdDao = PersonPseudoIdDaoFake()
    override val dokumentDao = DokumentDaoFake()
    override val yrkesaktivitetDao = YrkesaktivitetDaoOverRepository(yrkesaktivitetsperiodeRepository)
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoFake()
    override val beregningDao = UtbetalingsberegningDaoFake()
    override val outboxDao = OutboxDaoFake()
}

val sessionsDaoer = ConcurrentHashMap<String, FakeDaoer>()
val sessionsBrukere = ConcurrentHashMap<String, Bruker>()
private val helsesjekkPaths = setOf("/isalive", "/isready")

private fun ApplicationCall.erHelsesjekk() = request.path() in helsesjekkPaths

private fun ApplicationCall.erApiKall() = request.path().startsWith("/v1") || request.path().startsWith("/v2") || request.path().startsWith("/v2")

class DbDaoerFake : DbDaoer<AlleDaoer> {
    private suspend fun hentSessionDaoer(): AlleDaoer = sessionsDaoer[hentSession()] ?: throw IllegalStateException("Ingen Daoer funnet for session")

    override suspend fun <RET> nonTransactional(block: suspend (AlleDaoer.() -> RET)): RET = block(hentSessionDaoer())

    override suspend fun <RET> transactional(
        eksisterendeTransaksjon: AlleDaoer?,
        block: suspend (AlleDaoer.() -> RET),
    ): RET = block(eksisterendeTransaksjon ?: hentSessionDaoer())
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
            provider("entraid") {
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

        val providers = skapProviders(alleTestdata)
        val db = DbDaoerFake()
        val services = createServices(providers, db)

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
                        call.sessions.set("bakrommet-demo-session", sessionid)
                        // Opprett testdata asynkront for å unngå å blokkere requesten
                        val ctx = CoroutineSessionContext(sessionid)
                        application.launch {
                            try {
                                withContext(ctx) {
                                    services.opprettTestdata(alleTestdata)
                                }
                            } catch (e: Exception) {
                                appLogger.error("Feil ved opprettelse av testdata for session $sessionid", e)
                            }
                        }
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
            authenticate("entraid") {
                install(RolleMatrise)
                setupApiRoutes(services, db, providers)
            }
        }

        appLogger.info("Starter bakrommet")
    }.start(true)
}

fun Application.helsesjekker() {
    routing {
        get("/isready") {
            call.respondText("READY", ContentType.Text.Plain)
        }
        get("/isalive") {
            call.respondText("ALIVE", ContentType.Text.Plain)
        }
    }
}
