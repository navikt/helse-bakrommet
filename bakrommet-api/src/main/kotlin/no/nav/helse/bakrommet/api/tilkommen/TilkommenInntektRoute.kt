package no.nav.helse.bakrommet.api.tilkommen

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.api.PARAM_PERSONID
import no.nav.helse.bakrommet.api.dto.tilkommen.OpprettTilkommenInntektRequestDto
import no.nav.helse.bakrommet.api.periodeReferanse
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.api.tilkommenInntektReferanse
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektService
import no.nav.helse.bakrommet.person.PersonService

fun Route.tilkommenInntektRoute(
    service: TilkommenInntektService,
    personService: PersonService,
) {
    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/tilkommeninntekt") {
        get {
            val ref = call.periodeReferanse(personService)
            val tilkommenInntektDbRecords = service.hentTilkommenInntekt(ref)
            call.respondJson(tilkommenInntektDbRecords.map { it.tilTilkommenInntektResponseDto() })
        }
        post {
            val ref = call.periodeReferanse(personService)
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
            val ref = call.tilkommenInntektReferanse(personService)
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
            val ref = call.tilkommenInntektReferanse(personService)

            service.slettTilkommenInntekt(
                ref = ref,
                saksbehandler = call.saksbehandler(),
            )
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
