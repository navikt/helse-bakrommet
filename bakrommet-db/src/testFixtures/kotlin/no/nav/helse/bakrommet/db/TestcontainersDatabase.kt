package no.nav.helse.bakrommet.db

import org.testcontainers.containers.PostgreSQLContainer

internal class TestcontainersDatabase(
    moduleLabel: String,
) {
    private val postgres =
        PostgreSQLContainer("postgres:17")
            .withCommand("postgres", "-c", "max_connections=200")
            .withReuse(true)
            .withLabel("app", "bakrommet")
            .withLabel("module", moduleLabel)
            .withLabel("code-location", javaClass.canonicalName)
            .apply {
                start()
                println("Database startet opp.\nUrl: $jdbcUrl\nBrukernavn: $username\nPassord: $password")
            }

    val dbModuleConfiguration = DBModule.Configuration(jdbcUrl = postgres.jdbcUrl + "&user=" + postgres.username + "&password=" + postgres.password)
}
