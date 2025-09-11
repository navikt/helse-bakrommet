package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
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
        val db = MedDataSource(dbModule.dataSource)
        val cascade =
            if (testcontainers) {
                " cascade"
            } else {
                ""
            }
        if (!testcontainers) {
            db.execute("SET REFERENTIAL_INTEGRITY TO FALSE")
        }

        db.execute("truncate table kafka_outbox$cascade")
        db.execute("truncate table saksbehandlingsperiode$cascade")
        db.execute("truncate table ident$cascade")
        db.execute("truncate table yrkesaktivitet$cascade")

        if (!testcontainers) {
            db.execute("SET REFERENTIAL_INTEGRITY TO TRUE")
        }
    }
}
