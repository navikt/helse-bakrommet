package no.nav.helse.bakrommet.db

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

open class ModuleIsolatedDbTestFixture(
    moduleLabel: String,
) {
    private val database = TestcontainersDatabase(moduleLabel)

    val module = DBModule(database.dbModuleConfiguration)
    private val flywayMigrator = FlywayMigrator(database.dbModuleConfiguration)

    init {
        flywayMigrator.migrate()
        truncate()
    }

    private fun truncate() {
        sessionOf(module.dataSource).use {
            @Language("PostgreSQL")
            val query =
                """
                CREATE OR REPLACE FUNCTION truncate_tables() RETURNS void AS $$
                DECLARE
                truncate_statement text;
                BEGIN
                    SELECT 'TRUNCATE ' || string_agg(format('%I.%I', schemaname, tablename), ',') || ' RESTART IDENTITY CASCADE'
                        INTO truncate_statement
                    FROM pg_tables
                    WHERE schemaname='public'
                    AND tablename not in ('flyway_schema_history');

                    EXECUTE truncate_statement;
                END;
                $$ LANGUAGE plpgsql;
                """.trimIndent()
            it.run(queryOf(query).asExecute)
            it.run(queryOf("SELECT truncate_tables()").asExecute)
        }
    }
}
