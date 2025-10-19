package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.saksbehandlingsperiode.periodeReferanse
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.sykepengegrunnlagRoute(service: SykepengegrunnlagService) {
    route("/v2/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/sykepengegrunnlag") {
        get {
            val grunnlag = service.hentSykepengegrunnlag(call.periodeReferanse())
            call.respondText(
                grunnlag?.serialisertTilString() ?: "null",
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }
    }
}
