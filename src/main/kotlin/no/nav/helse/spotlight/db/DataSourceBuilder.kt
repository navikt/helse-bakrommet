package no.nav.helse.spotlight.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.spotlight.Configuration
import java.time.Duration
import javax.sql.DataSource

class DataSourceBuilder(configuration: Configuration.Database) {
    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = configuration.jdbcUrl
            username = configuration.username
            password = configuration.password
            maximumPoolSize = 5
            minimumIdle = 2
            idleTimeout = Duration.ofMinutes(1).toMillis()
            maxLifetime = idleTimeout * 5
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            connectionTimeout = Duration.ofSeconds(30).toMillis()
            metricRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        }

    fun build(): DataSource = HikariDataSource(hikariConfig)
}
