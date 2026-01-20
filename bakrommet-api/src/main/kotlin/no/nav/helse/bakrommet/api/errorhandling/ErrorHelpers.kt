package no.nav.helse.bakrommet.api.errorhandling

import no.nav.helse.bakrommet.errorhandling.InputValideringException

fun ugyldigInput(message: String): Nothing = throw InputValideringException(message)
