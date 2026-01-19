package no.nav.helse.bakrommet.api.errorhandling

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import no.nav.helse.bakrommet.errorhandling.ProblemDetails
import no.nav.helse.bakrommet.serialisertTilString

internal suspend fun ApplicationCall.respondProblem(
    status: HttpStatusCode,
    problem: ProblemDetails,
) {
    response.headers.append(HttpHeaders.ContentType, "application/problem+json")
    response.status(status)
    respondText(problem.serialisertTilString())
}

internal fun buildProblem(
    status: HttpStatusCode,
    title: String?,
    detail: String?,
    typePath: String,
    host: String = "https://spillerom.ansatt.nav.no",
    instance: String?,
) = ProblemDetails(
    type = "$host/$typePath",
    title = title,
    status = status.value,
    detail = detail,
    instance = instance,
)
