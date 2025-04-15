package no.nav.helse.bakrommet.infrastruktur.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FlywayMigrator(configuration: DBModule.Configuration) {
    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = configuration.jdbcUrl
            username = configuration.username
            password = configuration.password
            connectionTimeout = 5.seconds.inWholeMilliseconds
            initializationFailTimeout = 1.minutes.inWholeMilliseconds
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
