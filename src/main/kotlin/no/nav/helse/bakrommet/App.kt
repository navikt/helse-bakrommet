package no.nav.helse.bakrommet

import com.fasterxml.jackson.databind.JsonNode
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
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.alder
import no.nav.helse.bakrommet.pdl.formattert
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.sikkerLogger
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.*
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

    routing {
        authenticate("entraid") {
            post("/v1/personsok") {
                val ident = call.receive<JsonNode>()["ident"].asText()
                // Ident må være 11 eller 13 siffer lang
                if (ident.length != 11 && ident.length != 13) {
                    call.respond(HttpStatusCode.BadRequest, "Ident må være 11 eller 13 siffer lang")
                    return@post
                }

                val oboToken =
                    oboClient.exchangeToken(
                        bearerToken = call.request.bearerToken(),
                        scope = configuration.pdl.scope,
                    )

                val identer = pdlClient.hentIdenterFor(pdlToken = oboToken, ident = ident)

                fun hentEllerOpprettPersonid(naturligIdent: String): String {
                    personDao.finnPersonId(*identer.toTypedArray())?.let { return it }
                    val newPersonId = UUID.randomUUID().toString().replace("-", "").substring(0, 5)

                    // TODO naturlig ident her må være gjeldende fnr fra hentIdenter
                    personDao.opprettPerson(naturligIdent, newPersonId)
                    return newPersonId
                }

                call.response.headers.append("identer", identer.toString())
                call.response.headers.append("Content-Type", "application/json")
                call.respondText("""{ "personId": "${hentEllerOpprettPersonid(ident)}" }""")
            }
            get("/v1/{personId}/personinfo") {
                call.medIdent(personDao) { fnr, personId ->
                    val oboToken =
                        oboClient.exchangeToken(
                            bearerToken = call.request.bearerToken(),
                            scope = configuration.pdl.scope,
                        )

                    val hentPersonInfo =
                        pdlClient.hentPersonInfo(
                            pdlToken = oboToken,
                            ident = fnr,
                        )
                    val identer = pdlClient.hentIdenterFor(oboToken, fnr)

                    data class PersonInfo(
                        val fødselsnummer: String,
                        val aktørId: String,
                        val navn: String,
                        val alder: Int?,
                    )

                    val personInfo =
                        PersonInfo(
                            fødselsnummer = fnr,
                            aktørId = identer.first { it.length == 13 },
                            navn = hentPersonInfo.navn.formattert(),
                            alder = hentPersonInfo.alder(),
                        )

                    call.respondText(personInfo.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
                }
            }
            get("/v1/{personId}/soknader") {
                call.medIdent(personDao) { fnr, personId ->
                    val oboToken =
                        oboClient.exchangeToken(
                            bearerToken = call.request.bearerToken(),
                            scope = configuration.sykepengesoknadBackend.scope,
                        )
                    val soknader: List<SykepengesoknadDTO> =
                        sykepengesoknadBackendClient.hentSoknader(
                            sykepengesoknadToken = oboToken,
                            fnr = fnr,
                        )
                    call.respondText(soknader.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
                }
            }

            get("/v1/{personId}/dokumenter") {
                call.respondText("[]", ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }
}

private fun RoutingRequest.bearerToken(): String {
    val authHeader = headers["Authorization"]!!
    val token = authHeader.removePrefix("Bearer ").trim()
    return token
}
