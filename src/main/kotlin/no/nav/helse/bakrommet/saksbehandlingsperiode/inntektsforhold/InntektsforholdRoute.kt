package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_INNTEKTSFORHOLDUUID
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.saksbehandlingsperiode.periodeReferanse
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.util.UUID

fun RoutingCall.inntektsforholdReferanse() =
    InntektsforholdReferanse(
        saksbehandlingsperiodeReferanse = periodeReferanse(),
        inntektsforholdUUID = parameters[PARAM_INNTEKTSFORHOLDUUID].somGyldigUUID(),
    )

data class InntektsforholdDTO(
    val id: UUID,
    val kategorisering: JsonNode,
    val dagoversikt: JsonNode?,
    val generertFraDokumenter: List<UUID>,
)

fun Inntektsforhold.tilDto() =
    InntektsforholdDTO(
        id = id,
        kategorisering = kategorisering,
        dagoversikt = dagoversikt,
        generertFraDokumenter = generertFraDokumenter,
    )

internal fun Route.saksbehandlingsperiodeInntektsforholdRoute(service: InntektsforholdService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/inntektsforhold") {
        get {
            val inntektsforhold = service.hentInntektsforholdFor(call.periodeReferanse())
            call.respondText(
                inntektsforhold.map { it.tilDto() }.serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        post {
            val inntektsforholdRequest = call.receive<InntektsforholdCreateRequest>()
            val inntektsforhold =
                service.opprettInntektsforhold(
                    call.periodeReferanse(),
                    inntektsforholdRequest.kategorisering,
                    call.saksbehandler(),
                )
            call.respondText(
                inntektsforhold.tilDto().serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.Created,
            )
        }

        route("/{$PARAM_INNTEKTSFORHOLDUUID}") {
            delete {
                service.slettInntektsforhold(call.inntektsforholdReferanse(), call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/dagoversikt") {
                val dagerSomSkalOppdateres = call.receive<DagerSomSkalOppdateres>()
                service.oppdaterDagoversiktDager(call.inntektsforholdReferanse(), dagerSomSkalOppdateres, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/kategorisering") {
                val kategorisering = call.receive<InntektsforholdKategorisering>()
                service.oppdaterKategorisering(call.inntektsforholdReferanse(), kategorisering, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

data class InntektsforholdCreateRequest(
    val kategorisering: JsonNode,
)
