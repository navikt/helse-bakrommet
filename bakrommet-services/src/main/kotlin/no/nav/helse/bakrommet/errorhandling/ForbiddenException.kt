package no.nav.helse.bakrommet.errorhandling

class ForbiddenException(
    message: String,
) : ApplicationException(message = message)
