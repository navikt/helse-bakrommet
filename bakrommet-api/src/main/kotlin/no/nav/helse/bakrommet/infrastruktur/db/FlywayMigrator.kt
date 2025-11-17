package no.nav.helse.bakrommet.infrastruktur.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.november
import org.flywaydb.core.Flyway
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FlywayMigrator(
    configuration: Configuration.DB,
) {
    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = configuration.jdbcUrl
            connectionTimeout = 5.seconds.inWholeMilliseconds
            initializationFailTimeout = 1.minutes.inWholeMilliseconds
            maximumPoolSize = 2
        }

    val tillattNukeFør = ZonedDateTime.of(17.november(2025), LocalTime.of(20, 0, 0), ZoneId.of("Europe/Oslo")).toInstant()

    val nukeDb = Instant.now().isBefore(tillattNukeFør)

    fun migrate() {
        HikariDataSource(hikariConfig).use { dataSource ->
            Flyway
                .configure()
                .validateMigrationNaming(true)
                .dataSource(dataSource)
                .cleanDisabled(!nukeDb)
                .lockRetryCount(-1)
                .load()
                .also {
                    if (nukeDb) {
                        it.clean()
                    }
                }.migrate()
        }
    }
}
