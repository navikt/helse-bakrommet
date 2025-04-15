package no.nav.helse.spotlight

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spotlight.db.DataSourceBuilder
import no.nav.helse.spotlight.db.FlywayMigrator
import no.nav.helse.spotlight.db.TransactionManager
import no.nav.helse.spotlight.river.*
import no.nav.helse.spotlight.slack.SlackClient
import javax.sql.DataSource

val objectMapper: ObjectMapper =
    jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

data class Configuration(
    val database: Database,
    val slack: Slack,
) {
    data class Database(
        val jdbcUrl: String,
        val username: String,
        val password: String,
    )

    data class Slack(
        val accessToken: String,
        val channel: String,
        val url: String,
    )
}

class App(
    slackConfiguration: Configuration.Slack,
    dataSource: DataSource,
    rapidsConnection: RapidsConnection,
) {
    private val transactionManager = TransactionManager(dataSource)

    private val slackClient = SlackClient(slackConfiguration)

    private val rivers =
        listOf(
            KommandokjedeFerdigstiltRiver(transactionManager),
            KommandokjedeSuspendertRiver(transactionManager),
            KommandokjedeAvbruttRiver(transactionManager),
            KlokkaSeksHverdagerRiver(transactionManager, slackClient),
            HverHalvtimeRiver(transactionManager, rapidsConnection),
        )

    fun buildRivers(rapidsConnection: RapidsConnection) {
        rivers.forEach { it.buildRiver(rapidsConnection) }
    }
}

fun main() {
    val env = System.getenv()
    val configuration =
        Configuration(
            Configuration.Database(
                jdbcUrl =
                    "jdbc:postgresql://" +
                        env.getRequired("DATABASE_HOST") +
                        ":" +
                        env.getRequired("DATABASE_PORT") +
                        "/" +
                        env.getRequired("DATABASE_DATABASE"),
                username = env.getRequired("DATABASE_USERNAME"),
                password = env.getRequired("DATABASE_PASSWORD"),
            ),
            Configuration.Slack(
                accessToken = env.getRequired("SLACK_SPY_ACCESS_TOKEN"),
                channel = env.getRequired("SLACK_SPOTLIGHT_CHANNEL_ID"),
                url = "https://slack.com/api/chat.postMessage",
            ),
        )
    start(
        dataSource = DataSourceBuilder(configuration.database).build(),
        rapidsConnection =
            RapidApplication.create(env).apply {
                register(
                    object : RapidsConnection.StatusListener {
                        override fun onStartup(rapidsConnection: RapidsConnection) {
                            FlywayMigrator(configuration.database).migrate()
                        }
                    },
                )
            },
        slackConfiguration = configuration.slack,
    )
}

fun start(
    dataSource: DataSource,
    rapidsConnection: RapidsConnection,
    slackConfiguration: Configuration.Slack,
): App =
    App(slackConfiguration, dataSource, rapidsConnection).also {
        it.buildRivers(rapidsConnection)
        rapidsConnection.start()
    }

fun Map<String, String>.getRequired(name: String) = this[name] ?: error("$name må settes")
