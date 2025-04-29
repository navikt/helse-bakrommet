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
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.pdl.PdlClient
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import javax.sql.DataSource

val appLogger = LoggerFactory.getLogger("bakrommet")
val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

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
) {
    azureAdAppAuthentication(configuration.auth)
    helsesjekker()
    appModul(dataSource, oboClient, pdlClient, configuration)
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
) {
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter())
    }
    install(CallLogging) {
        disableDefaultColors()
        logger = sikkerLogger
        level = Level.INFO
    }

    routing {
        authenticate("oidc") {
            get("/antallBehandlinger") {
                call.respondText { dataSource.query("select count(*) from behandling")!! }
            }
            post("/v1/personsok") {
                val fnr = call.receive<JsonNode>()["fødselsnummer"].asText()

                val oboToken =
                    oboClient.exchangeToken(
                        bearerToken = call.request.bearerToken(),
                        scope = configuration.pdl.scope,
                    )

                val identer = pdlClient.hentIdenterFor(pdlToken = oboToken, ident = fnr)

                call.response.headers.append("identer", identer.toString())
                call.response.headers.append("Content-Type", "application/json")
                call.respondText("""{ "personId": "abc12" }""")
            }
            get("/v1/{personId}/personinfo") {
                call.response.headers.append("Content-Type", "application/json")
                call.respondText(
                    """
                    {
                        "fødselsnummer": "62345678906",
                        "aktørId": "1234567891011",
                        "navn": "Kalle Bakrommet Kranfører",
                        "alder": 47
                    }
                    """.trimIndent(),
                )
            }
            get("/v1/{personId}/soknader") {
                call.response.headers.append("Content-Type", "application/json")
                call.respondText("[]")
            }
        }
    }
}

private fun RoutingRequest.bearerToken(): String {
    val authHeader = headers["Authorization"]!!
    val token = authHeader.removePrefix("Bearer ").trim()
    return token
}

private fun DataSource.query(
    @Language("postgresql") sql: String,
) = sessionOf(this, strict = true)
    .use { session ->
        session.run(queryOf(sql).map { it.string(1) }.asSingle)
    }
