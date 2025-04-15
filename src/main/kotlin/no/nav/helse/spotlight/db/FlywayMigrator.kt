package no.nav.helse.spotlight.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spotlight.Configuration
import org.flywaydb.core.Flyway
import java.time.Duration

class FlywayMigrator(configuration: Configuration.Database) {
    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = configuration.jdbcUrl
            username = configuration.username
            password = configuration.password
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            connectionTimeout = Duration.ofMinutes(1).toMillis()
            maximumPoolSize = 2
        }

    fun migrate() {
        HikariDataSource(hikariConfig).use { dataSource ->
            Flyway.configure()
                .dataSource(dataSource)
                .lockRetryCount(-1)
                .load()
                .migrate()
        }
    }
}
