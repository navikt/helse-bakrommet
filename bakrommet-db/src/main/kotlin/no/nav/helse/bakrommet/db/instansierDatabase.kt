package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.db.DBModule

fun instansierDatabase(configuration: Configuration.DB) = DBModule(configuration = configuration).also { it.migrate() }.dataSource
