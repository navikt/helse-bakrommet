package no.nav.helse.bakrommet.errorhandling

sealed class ApplicationException(
    message: String? = null,
) : RuntimeException(message)
