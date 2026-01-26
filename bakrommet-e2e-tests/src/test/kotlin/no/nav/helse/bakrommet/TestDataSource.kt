package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.db.DBModule
import no.nav.helse.bakrommet.db.MedDataSource
import org.testcontainers.containers.PostgreSQLContainer

object TestDataSource {
    val postgres: PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:17")
            .withCommand("postgres", "-c", "max_connections=200")
            .withReuse(true)
            .withLabel("app", "bakrommet")
            .apply {
                start()
                println("Database startet opp.\nUrl: $jdbcUrl\nBrukernavn: $username\nPassord: $password")
            }

    val dbModule =
        DBModule(
            configuration =
                DBModule.Configuration(
                    jdbcUrl = postgres.jdbcUrl + "&user=" + postgres.username + "&password=" + postgres.password,
                ),
        ).also { it.migrate() }

    fun resetDatasource() {
        val db = MedDataSource(dbModule.dataSource)
        db.execute("truncate table kafka_outbox cascade")
        db.execute("truncate table behandling cascade")
        db.execute("truncate table person_pseudo_id cascade")
        db.execute("truncate table yrkesaktivitet cascade")
        db.execute("truncate table dokument cascade")
        db.execute("truncate table tilkommen_inntekt cascade")
    }
}
