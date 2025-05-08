package no.nav.helse.bakrommet.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

class H2TestDb {
    companion object {
        fun createMigratedEmptyDataSource(version: String? = null): HikariDataSource {
            val hikariConfig =
                HikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
                    maximumPoolSize = 2
                }
            val flyDS = HikariDataSource(hikariConfig)
            val conf =
                Flyway.configure().dataSource(flyDS)
                    .locations("db/migration")
            if (version != null) {
                conf.target(version)
            }
            conf.load()
                .migrate()
            flyDS.connection.use { connection ->
                connection.prepareStatement("truncate table saksbehandlingsperiode").executeUpdate()
                // connection.prepareStatement("truncate table ident").executeUpdate()
            }

            return flyDS
        }
    }
}
