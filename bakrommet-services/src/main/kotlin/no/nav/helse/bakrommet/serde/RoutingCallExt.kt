package no.nav.helse.bakrommet.serde

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import no.nav.helse.bakrommet.util.serialisertTilString

suspend fun RoutingCall.respondJson(
    data: Any,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondText(data.serialisertTilString(), ContentType.Application.Json, status)
}
