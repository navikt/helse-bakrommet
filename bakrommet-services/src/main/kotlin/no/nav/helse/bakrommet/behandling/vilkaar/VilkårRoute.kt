package no.nav.helse.bakrommet.behandling.vilkaar

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.serde.respondJson

fun VurdertVilkår.tilApiSvar(): Vilkaarsvurdering = vurdering

internal fun Route.saksbehandlingsperiodeVilkårRoute(service: VilkårService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/vilkaarsvurdering") {
        get {
            val vurderteVilkår =
                service.hentVilkårsvurderingerFor(call.periodeReferanse()).map {
                    it.tilApiSvar()
                }
            call.respondJson(vurderteVilkår)
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/vilkaarsvurdering/{hovedspørsmål}") {
        put {
            val request = call.receive<VilkaarsvurderingRequest>()
            val (lagretVurdering, opprettetEllerEndret) =
                service.leggTilEllerOpprettVurdertVilkår(
                    ref = call.periodeReferanse(),
                    vilkårsKode = Kode(call.parameters["hovedspørsmål"]!!),
                    request = request,
                    saksbehandler = call.saksbehandler(),
                )
            call.respondJson(
                lagretVurdering.tilApiSvar(),
                status = if (opprettetEllerEndret == OpprettetEllerEndret.OPPRETTET) HttpStatusCode.Created else HttpStatusCode.OK,
            )
        }

        delete {
            val bleFunnetOgSlettet =
                service.slettVilkårsvurdering(
                    ref = call.periodeReferanse(),
                    vilkårsKode = Kode(call.parameters["hovedspørsmål"]!!),
                    saksbehandler = call.saksbehandler(),
                )
            if (bleFunnetOgSlettet) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
