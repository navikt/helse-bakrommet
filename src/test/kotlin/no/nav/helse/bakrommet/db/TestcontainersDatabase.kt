package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import org.testcontainers.containers.PostgreSQLContainer

class TestcontainersDatabase {
    private val postgres =
        PostgreSQLContainer("postgres:17")
            .withReuse(true)
            .withLabel("app", "bakrommet")
            .withLabel("code-location", javaClass.canonicalName)
            .apply {
                start()
                println("Database startet opp.\nUrl: $jdbcUrl\nBrukernavn: $username\nPassord: $password")
            }

    val dbModuleConfiguration =
        DBModule.Configuration(
            jdbcUrl = postgres.jdbcUrl + "&user=" + postgres.username + "&password=" + postgres.password,
        )
}
