package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.util.execute

object TestDataSource {
    val dataSource = DBModule(configuration = TestcontainersDatabase.configuration).also { it.migrate() }.dataSource

    init {
        dataSource.execute(
            """
            DO $$
            DECLARE
                r RECORD;
            BEGIN
                FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename not in ('flyway_schema_history')) LOOP
                    EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' CASCADE';
                END LOOP;
            END $$;
            """.trimIndent(),
        )
    }
}
