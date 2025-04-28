package no.nav.helse.bakrommet

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

fun main() {
    val env = System.getenv()
    val dbConfiguration = DBModule.Configuration(env.getValue("DATABASE_JDBC_URL"))
    val authConfiguration = authConfig()

    startApp(dbConfiguration, authConfiguration)
}

internal fun startApp(
    dbModule: DBModule.Configuration,
    authConfiguration: AuthConfiguration,
) {
    appLogger.info("Setter opp data source")
    val dataSource = instansierDatabase(dbModule)

    embeddedServer(CIO, port = 8080) {
        appLogger.info("Setter opp ktor")
        settOppKtor(dataSource, authConfiguration)
        appLogger.info("Starter bakrommet")
    }.start(true)
}

internal fun instansierDatabase(configuration: DBModule.Configuration) =
    DBModule(configuration = configuration).also { it.migrate() }.dataSource

internal fun Application.settOppKtor(
    dataSource: DataSource,
    authConfiguration: AuthConfiguration,
) {
    azureAdAppAuthentication(authConfiguration)
    helsesjekker()
    appModul(dataSource)
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

internal fun Application.appModul(dataSource: DataSource) {
    routing {
        authenticate("oidc") {
            get("/antallBehandlinger") {
                call.respondText { dataSource.query("select count(*) from behandling")!! }
            }
            post("/v1/personsok") {
                call.response.headers.append("Content-Type", "application/json")
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
