package no.nav.helse.bakrommet.errorhandling

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import no.nav.helse.bakrommet.util.serialisertTilString

suspend fun ApplicationCall.respondProblem(
    status: HttpStatusCode,
    problem: ProblemDetails,
) {
    response.headers.append(HttpHeaders.ContentType, "application/problem+json")
    response.status(status)
    respondText(problem.serialisertTilString())
}

fun buildProblem(
    status: HttpStatusCode,
    detail: String?,
    typePath: String,
    host: String = "https://spillerom.ansatt.nav.no",
    instance: String?,
) = ProblemDetails(
    type = "$host/$typePath",
    title = status.description,
    status = status.value,
    detail = detail,
    instance = instance,
)
