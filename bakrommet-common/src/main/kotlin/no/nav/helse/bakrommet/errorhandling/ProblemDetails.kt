package no.nav.helse.bakrommet.errorhandling

import io.ktor.server.application.*

/**
 * Minimal implementasjon av RFC 9457 / 7807.
 * Alle felt er optional bortsett fra `status`.
 */
data class ProblemDetails(
    val type: String = "about:blank",
    val title: String? = null,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
)

interface ProblemDetailsAware {
    fun toProblemDetails(call: ApplicationCall): ProblemDetails
}

abstract class ProblemDetailsException(
    message: String? = null,
) : RuntimeException(message), ProblemDetailsAware
