package no.nav.helse.spotlight

import no.nav.helse.spotlight.db.DataSourceBuilder
import no.nav.helse.spotlight.db.FlywayMigrator
import org.testcontainers.containers.PostgreSQLContainer

object TestDatabase {
    private val postgres =
        PostgreSQLContainer("postgres:15").apply {
            withReuse(true)
            withLabel("app-navn", "spotlight")
            start()
            println("Database: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
        }

    private val configuration =
        Configuration.Database(
            jdbcUrl = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password,
        )

    val dataSource = DataSourceBuilder(configuration).build()

    init {
        FlywayMigrator(configuration).migrate()
    }
}
