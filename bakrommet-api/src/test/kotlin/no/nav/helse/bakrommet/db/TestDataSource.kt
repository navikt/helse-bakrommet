package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import org.testcontainers.containers.PostgreSQLContainer

object TestDataSource {
    val postgres =
        PostgreSQLContainer("postgres:17")
            .withReuse(true)
            .withLabel("app", "bakrommet")
            .apply { start() }

    val configuration =
        Configuration.DB(
            jdbcUrl = postgres.jdbcUrl + "&user=" + postgres.username + "&password=" + postgres.password,
        )

    val dbModule =
        DBModule(
            configuration =
                Configuration.DB(
                    jdbcUrl = postgres.jdbcUrl + "&user=" + postgres.username + "&password=" + postgres.password,
                ),
        ).also { it.migrate() }

    fun resetDatasource() {
        val db = MedDataSource(dbModule.dataSource)
        db.execute("truncate table kafka_outbox cascade")
        db.execute("truncate table behandling cascade")
        db.execute("truncate table ident cascade")
        db.execute("truncate table yrkesaktivitet cascade")
        db.execute("truncate table dokument cascade")
    }
}
