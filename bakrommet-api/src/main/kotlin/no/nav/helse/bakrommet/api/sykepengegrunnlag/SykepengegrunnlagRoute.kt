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
import no.nav.helse.bakrommet.api.PARAM_BEHANDLING_ID
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.auth.bruker
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.OpprettSykepengegrunnlagRequestDto
import no.nav.helse.bakrommet.api.periodeReferanse
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagService
import no.nav.helse.bakrommet.person.PersonService

fun Route.sykepengegrunnlagRoute(
    service: SykepengegrunnlagService,
    personService: PersonService,
) {
    route("/v2/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/sykepengegrunnlag") {
        get {
            val grunnlag = service.hentSykepengegrunnlag(call.periodeReferanse(personService))
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
                    referanse = call.periodeReferanse(personService),
                    saksbehandler = call.bruker(),
                )
            val response = grunnlag.tilSykepengegrunnlagResponseDto()
            call.respondJson(response, status = HttpStatusCode.Created)
        }

        delete {
            val ref = call.periodeReferanse(personService)
            service.slettSykepengegrunnlag(
                ref = ref,
                saksbehandler = call.bruker(),
            )
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
