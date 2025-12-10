package no.nav.helse.bakrommet.api.vilkaar

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.api.PARAM_PERSONID
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingRequestDto
import no.nav.helse.bakrommet.api.periodeReferanse
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.behandling.vilkaar.Kode
import no.nav.helse.bakrommet.behandling.vilkaar.OpprettetEllerEndret
import no.nav.helse.bakrommet.behandling.vilkaar.VilkårService
import no.nav.helse.bakrommet.person.PersonService

fun Route.vilkårRoute(
    service: VilkårService,
    personService: PersonService,
) {
    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/vilkaarsvurdering") {
        get {
            val vurderteVilkår =
                service.hentVilkårsvurderingerFor(call.periodeReferanse(personService)).map {
                    it.tilVilkaarsvurderingDto()
                }
            call.respondJson(vurderteVilkår)
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/vilkaarsvurdering/{hovedspørsmål}") {
        put {
            val request = call.receive<VilkaarsvurderingRequestDto>()
            val (lagretVurdering, opprettetEllerEndret) =
                service.leggTilEllerOpprettVurdertVilkår(
                    ref = call.periodeReferanse(personService),
                    vilkårsKode = Kode(call.parameters["hovedspørsmål"]!!),
                    request = request.tilVilkaarsvurderingRequest(),
                    saksbehandler = call.saksbehandler(),
                )
            call.respondJson(
                lagretVurdering.tilVilkaarsvurderingDto(),
                status = if (opprettetEllerEndret == OpprettetEllerEndret.OPPRETTET) HttpStatusCode.Created else HttpStatusCode.OK,
            )
        }

        delete {
            val bleFunnetOgSlettet =
                service.slettVilkårsvurdering(
                    ref = call.periodeReferanse(personService),
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
