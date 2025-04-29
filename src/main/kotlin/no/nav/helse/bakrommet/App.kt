package no.nav.helse.bakrommet

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
) {
    val httpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        }
    azureAdAppAuthentication(configuration.auth)
    helsesjekker()
    appModul(dataSource, httpClient, PdlClient(configuration.pdl), configuration)
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
    httpClient: HttpClient,
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

                val authHeader = call.request.headers["Authorization"]!!
                val token = authHeader.removePrefix("Bearer ").trim()
                val oboTokenResponse =
                    httpClient.post(configuration.obo.url) {
                        contentType(ContentType.Application.Json)
                        setBody(
                            jacksonObjectMapper().createObjectNode().apply {
                                put("identity_provider", "azuread")
                                put("target", "api://${configuration.pdl.scope}/.default")
                                put("user_token", token)
                            }.toString(),
                        )
                    }
                if (!oboTokenResponse.status.isSuccess()) {
                    sikkerLogger.warn(oboTokenResponse.bodyAsText())
                }
                call.response.headers.append("Content-Type", "application/json")
                call.response.headers.append("Obo", oboTokenResponse.bodyAsText())

                val jsonResponse = jacksonObjectMapper().readTree(oboTokenResponse.bodyAsText())
                val oboToken = jsonResponse["access_token"].asText()

                pdlClient.hentIdenterFor(pdlToken = oboToken, ident = fnr)

                call.respondText("""{ "personId": "abc12" }""")
            }
            get("/v1/{personId}/personinfo") {
                call.response.headers.append("Content-Type", "application/json")
                call.respondText(
                    """{
        "fødselsnummer": "62345678906",
        "aktørId": "1234567891011",
        "navn": "Kalle Bakrommet Kranfører",
        "alder": 47
    }""",
                )
            }
            get("/v1/{personId}/soknader") {
                call.response.headers.append("Content-Type", "application/json")
                call.respondText("[]")
            }
        }
    }
}

private fun DataSource.query(
    @Language("postgresql") sql: String,
) = sessionOf(this, strict = true)
    .use { session ->
        session.run(queryOf(sql).map { it.string(1) }.asSingle)
    }
