package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

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
    YrkesaktivitetReferanse(
        saksbehandlingsperiodeReferanse = periodeReferanse(),
        inntektsforholdUUID = parameters[PARAM_INNTEKTSFORHOLDUUID].somGyldigUUID(),
    )

data class YrkesaktivitetDTO(
    val id: UUID,
    val kategorisering: JsonNode,
    val dagoversikt: JsonNode?,
    val generertFraDokumenter: List<UUID>,
    val dekningsgrad: Int,
)

fun Yrkesaktivitet.tilDto() =
    YrkesaktivitetDTO(
        id = id,
        kategorisering = kategorisering,
        dagoversikt = dagoversikt,
        generertFraDokumenter = generertFraDokumenter,
        dekningsgrad = hentDekningsgrad().toDouble().toInt(),
    )

internal fun Route.saksbehandlingsperiodeYrkesaktivitetRoute(service: YrkesaktivitetService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/yrkesaktivitet") {
        get {
            val inntektsforhold = service.hentYrkesaktivitetFor(call.periodeReferanse())
            call.respondText(
                inntektsforhold.map { it.tilDto() }.serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        post {
            val inntektsforholdRequest = call.receive<YrkesaktivitetCreateRequest>()
            val inntektsforhold =
                service.opprettYrkesaktivitet(
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
                service.slettYrkesaktivitet(call.inntektsforholdReferanse(), call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/dagoversikt") {
                val dagerSomSkalOppdateres = call.receive<DagerSomSkalOppdateres>()
                service.oppdaterDagoversiktDager(call.inntektsforholdReferanse(), dagerSomSkalOppdateres, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/kategorisering") {
                val kategorisering = call.receive<YrkesaktivitetKategorisering>()
                service.oppdaterKategorisering(call.inntektsforholdReferanse(), kategorisering, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

data class YrkesaktivitetCreateRequest(
    val kategorisering: JsonNode,
)
