package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.Configuration
import org.testcontainers.containers.PostgreSQLContainer

object TestcontainersDatabase {
    private val postgres =
        PostgreSQLContainer("postgres:17")
            .withReuse(true)
            .withLabel("app", "bakrommet")
            .apply {
                start()
            }

    val configuration =
        Configuration.DB(
            jdbcUrl = postgres.jdbcUrl + "&user=" + postgres.username + "&password=" + postgres.password,
        )
}
