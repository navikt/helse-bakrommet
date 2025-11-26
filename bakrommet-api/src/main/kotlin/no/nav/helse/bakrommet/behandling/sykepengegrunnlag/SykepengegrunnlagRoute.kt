package no.nav.helse.bakrommet.behandling.sykepengegrunnlag

import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde

internal fun Route.sykepengegrunnlagRoute(service: SykepengegrunnlagService) {
    route("/v2/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/sykepengegrunnlag") {
        get {
            val grunnlag = service.hentSykepengegrunnlag(call.periodeReferanse())
            call.respondText(
                grunnlag?.let { objectMapperCustomSerde.writeValueAsString(it) } ?: "null",
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        post {
            val request = call.receive<OpprettSykepengegrunnlagRequest>()
            val grunnlag = service.opprettSykepengegrunnlag(request, call.periodeReferanse(), call.saksbehandler())
            call.respondText(
                objectMapperCustomSerde.writeValueAsString(grunnlag),
                ContentType.Application.Json,
                HttpStatusCode.Created,
            )
        }
    }
}
