package no.nav.helse.bakrommet.api.sykepengegrunnlag

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.OpprettSykepengegrunnlagRequestDto
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagService

fun Route.sykepengegrunnlagRoute(service: SykepengegrunnlagService) {
    route("/v2/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/sykepengegrunnlag") {
        get {
            val grunnlag = service.hentSykepengegrunnlag(call.periodeReferanse())
            if (grunnlag == null) {
                call.respondText("null", ContentType.Application.Json, HttpStatusCode.OK)
            } else {
                val response = grunnlag.tilSykepengegrunnlagResponseDto()
                call.respondJson(response)
            }
        }

        post {
            val requestDto = call.receive<OpprettSykepengegrunnlagRequestDto>()
            val request = requestDto.tilOpprettSykepengegrunnlagRequest()
            val grunnlag =
                service.opprettSykepengegrunnlag(
                    request = request,
                    referanse = call.periodeReferanse(),
                    saksbehandler = call.saksbehandler(),
                )
            val response = grunnlag.tilSykepengegrunnlagResponseDto()
            call.respondJson(response, status = HttpStatusCode.Created)
        }

        delete {
            val ref = call.periodeReferanse()
            service.slettSykepengegrunnlag(
                ref = ref,
                saksbehandler = call.saksbehandler(),
            )
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
