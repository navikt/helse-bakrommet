package no.nav.helse.bakrommet.api.serde

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import no.nav.helse.bakrommet.api.dto.tidslinje.ApiResponse
import no.nav.helse.bakrommet.util.serialisertTilString

internal suspend fun RoutingCall.respondJson(
    data: ApiResponse,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondText(data.serialisertTilString(), ContentType.Application.Json, status)
}
