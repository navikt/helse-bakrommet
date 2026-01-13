package no.nav.helse.bakrommet.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DBModule(
    configuration: Configuration,
    hikariConfigurator: (Configuration) -> HikariConfig = ::defaultHikariConfigurator,
) {
    data class Configuration(
        val jdbcUrl: String,
    )

    val jdbcUrl = configuration.jdbcUrl
    val dataSource = HikariDataSource(hikariConfigurator(configuration))
    private val flywayMigrator = FlywayMigrator(configuration)

    fun migrate() {
        flywayMigrator.migrate()
    }
}

private fun defaultHikariConfigurator(configuration: DBModule.Configuration) =
    HikariConfig().apply {
        jdbcUrl = configuration.jdbcUrl
        maximumPoolSize = 20
        minimumIdle = 2
        idleTimeout = 1.minutes.inWholeMilliseconds
        maxLifetime = idleTimeout * 5
        initializationFailTimeout = 1.minutes.inWholeMilliseconds
        connectionTimeout = 5.seconds.inWholeMilliseconds
        leakDetectionThreshold = 30.seconds.inWholeMilliseconds

        metricRegistry =
            PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT,
                PrometheusRegistry.defaultRegistry,
                Clock.SYSTEM,
            )
    }
