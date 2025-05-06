package no.nav.helse.bakrommet.errorhandling

import java.lang.RuntimeException

class InputValideringException(message: String) : RuntimeException(message)
