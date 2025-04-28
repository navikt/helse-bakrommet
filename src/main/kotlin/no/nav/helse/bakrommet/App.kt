package no.nav.helse.bakrommet

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import javax.sql.DataSource

val appLogger = LoggerFactory.getLogger("bakrommet")
val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

fun main() {
    val env = System.getenv()
    val dbConfiguration = DBModule.Configuration(env.getValue("DATABASE_JDBC_URL"))
    val authConfiguration = authConfig()
    val texasUrl = env.getValue("NAIS_TOKEN_EXCHANGE_ENDPOINT")
    val pdlScope = env.getValue("PDL_SCOPE")

    startApp(dbConfiguration, authConfiguration, texasUrl, pdlScope)
}

internal fun startApp(
    dbModule: DBModule.Configuration,
    authConfiguration: AuthConfiguration,
    texasUrl: String,
    pdlScope: String,
) {
    appLogger.info("Setter opp data source")
    val dataSource = instansierDatabase(dbModule)

    embeddedServer(CIO, port = 8080) {
        appLogger.info("Setter opp ktor")
        settOppKtor(dataSource, authConfiguration, texasUrl, pdlScope)
        appLogger.info("Starter bakrommet")
    }.start(true)
}

internal fun instansierDatabase(configuration: DBModule.Configuration) =
    DBModule(configuration = configuration).also { it.migrate() }.dataSource

internal fun Application.settOppKtor(
    dataSource: DataSource,
    authConfiguration: AuthConfiguration,
    texasUrl: String,
    pdlScope: String,
) {
    val httpClient =
        HttpClient {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        }
    azureAdAppAuthentication(authConfiguration)
    helsesjekker()
    appModul(dataSource, httpClient, texasUrl, pdlScope)
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
    texasUrl: String,
    pdlScope: String,
) {
    routing {
        authenticate("oidc") {
            get("/antallBehandlinger") {
                call.respondText { dataSource.query("select count(*) from behandling")!! }
            }
            post("/v1/personsok") {
                val authHeader = call.request.headers["Authorization"]!!
                val token = authHeader.removePrefix("Bearer ").trim()
                val oboTokenResponse =
                    httpClient.post(texasUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "identity_provider": "azuread",
                                "target": "api://$pdlScope/.default",
                                "user_token": "$token"
                            }
                            """.trimIndent(),
                        )
                    }
                if (!oboTokenResponse.status.isSuccess()) {
                    sikkerLogger.warn(oboTokenResponse.bodyAsText())
                }
                call.response.headers.append("Content-Type", "application/json")
                call.response.headers.append("Obo", oboTokenResponse.bodyAsText())
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
