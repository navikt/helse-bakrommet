package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.util.execute
import org.testcontainers.containers.PostgreSQLContainer

object TestDataSource {
    val testcontainers = System.getenv("TESTCONTAINERS") == "true"
    val dbModule =
        if (testcontainers) {
            val postgres =
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

            DBModule(configuration = configuration)
        } else {
            DBModule(
                configuration =
                    Configuration.DB(
                        jdbcUrl = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                    ),
            )
        }.also { it.migrate() }

    fun resetDatasource() {
        val cascade =
            if (testcontainers) {
                " cascade"
            } else {
                ""
            }
        if (!testcontainers) {
            dbModule.dataSource.execute("SET REFERENTIAL_INTEGRITY TO FALSE")
        }

        dbModule.dataSource.execute("truncate table saksbehandlingsperiode$cascade")
        dbModule.dataSource.execute("truncate table ident$cascade")

        if (!testcontainers) {
            dbModule.dataSource.execute("SET REFERENTIAL_INTEGRITY TO TRUE")
        }
    }
}
