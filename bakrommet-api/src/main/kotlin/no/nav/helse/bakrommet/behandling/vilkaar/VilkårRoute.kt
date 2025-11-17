package no.nav.helse.bakrommet.behandling.vilkaar

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.util.serialisertTilString

fun VurdertVilkår.tilApiSvar(): JsonNode {
    val kopiert = vurdering.deepCopy<ObjectNode>()
    kopiert.put("hovedspørsmål", kode)
    return kopiert
}

internal fun Route.saksbehandlingsperiodeVilkårRoute(service: VilkårService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/vilkaarsvurdering") {
        get {
            val vurderteVilkår =
                service.hentVilkårsvurderingerFor(call.periodeReferanse()).map {
                    it.tilApiSvar()
                }
            call.respondText(vurderteVilkår.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/vilkaarsvurdering/{hovedspørsmål}") {
        put {
            val (lagretVurdering, opprettetEllerEndret) =
                service.leggTilEllerOpprettVurdertVilkår(
                    ref = call.periodeReferanse(),
                    vilkårsKode = Kode(call.parameters["hovedspørsmål"]!!),
                    vurdertVilkår = call.receive<JsonNode>(),
                    saksbehandler = call.saksbehandler(),
                )
            call.respondText(
                lagretVurdering.tilApiSvar().serialisertTilString(),
                ContentType.Application.Json,
                if (opprettetEllerEndret == OpprettetEllerEndret.OPPRETTET) HttpStatusCode.Created else HttpStatusCode.OK,
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
