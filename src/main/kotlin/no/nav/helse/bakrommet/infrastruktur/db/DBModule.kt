package no.nav.helse.bakrommet.infrastruktur.db

class DBModule(configuration: Configuration) {
    data class Configuration(
        val jdbcUrl: String,
        val username: String,
        val password: String,
    )

    val dataSource = DataSourceBuilder(configuration).build()
    private val flywayMigrator = FlywayMigrator(configuration)

    fun migrate() {
        flywayMigrator.migrate()
    }
}
