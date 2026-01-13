package no.nav.helse.bakrommet.db

fun instansierDatabase(configuration: DBModule.Configuration) = DBModule(configuration = configuration).also { it.migrate() }.dataSource
