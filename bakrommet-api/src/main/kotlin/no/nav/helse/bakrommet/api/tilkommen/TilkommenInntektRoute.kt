package no.nav.helse.bakrommet.api.tilkommen

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.PARAM_TILKOMMENINNTEKT_ID
import no.nav.helse.bakrommet.api.dto.tilkommen.OpprettTilkommenInntektRequestDto
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektReferanse
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektService
import no.nav.helse.bakrommet.util.somGyldigUUID

fun RoutingCall.tilkommenInntektReferanse() =
    TilkommenInntektReferanse(
        behandling = periodeReferanse(),
        tilkommenInntektId = parameters[PARAM_TILKOMMENINNTEKT_ID].somGyldigUUID(),
    )

fun Route.tilkommenInntektRoute(service: TilkommenInntektService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/tilkommeninntekt") {
        get {
            val ref = call.periodeReferanse()
            val tilkommenInntektDbRecords = service.hentTilkommenInntekt(ref)
            call.respondJson(tilkommenInntektDbRecords.map { it.tilTilkommenInntektResponseDto() })
        }
        post {
            val ref = call.periodeReferanse()
            val request = call.receive<OpprettTilkommenInntektRequestDto>()
            val nyTilkommenInntekt =
                service.lagreTilkommenInntekt(
                    ref = ref,
                    tilkommenInntekt = request.tilTilkommenInntekt(),
                    saksbehandler = call.saksbehandler(),
                )
            call.respondJson(nyTilkommenInntekt.tilTilkommenInntektResponseDto(), status = HttpStatusCode.Created)
        }

        put("/{tilkommenInntektId}") {
            val ref = call.tilkommenInntektReferanse()
            val request = call.receive<OpprettTilkommenInntektRequestDto>()

            val oppdatertTilkommenInntekt =
                service.endreTilkommenInntekt(
                    ref = ref,
                    tilkommenInntekt = request.tilTilkommenInntekt(),
                    saksbehandler = call.saksbehandler(),
                )
            call.respondJson(oppdatertTilkommenInntekt.tilTilkommenInntektResponseDto())
        }

        delete("/{tilkommenInntektId}") {
            val ref = call.tilkommenInntektReferanse()

            service.slettTilkommenInntekt(
                ref = ref,
                saksbehandler = call.saksbehandler(),
            )
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
