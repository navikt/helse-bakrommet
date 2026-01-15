package no.nav.helse.bakrommet.errorhandling

class IkkeFunnetException(
    val title: String,
    detail: String = title,
) : ApplicationException(detail)
