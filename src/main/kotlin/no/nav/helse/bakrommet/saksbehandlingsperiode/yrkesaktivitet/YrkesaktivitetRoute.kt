package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.PARAM_YRKESAKTIVITETUUID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.saksbehandlingsperiode.periodeReferanse
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.util.UUID

fun RoutingCall.yrkesaktivitetReferanse() =
    YrkesaktivitetReferanse(
        saksbehandlingsperiodeReferanse = periodeReferanse(),
        yrkesaktivitetUUID = parameters[PARAM_YRKESAKTIVITETUUID].somGyldigUUID(),
    )

data class YrkesaktivitetDTO(
    val id: UUID,
    val kategorisering: JsonNode,
    val dagoversikt: JsonNode?,
    val generertFraDokumenter: List<UUID>,
)

fun Yrkesaktivitet.tilDto() =
    YrkesaktivitetDTO(
        id = id,
        kategorisering = kategorisering,
        dagoversikt = dagoversikt,
        generertFraDokumenter = generertFraDokumenter,
    )

internal fun Route.saksbehandlingsperiodeYrkesaktivitetRoute(service: YrkesaktivitetService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/yrkesaktivitet") {
        get {
            val yrkesaktivitet = service.hentYrkesaktivitetFor(call.periodeReferanse())
            call.respondText(
                yrkesaktivitet.map { it.tilDto() }.serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        post {
            val yrkesaktivitetRequest = call.receive<YrkesaktivitetCreateRequest>()
            val yrkesaktivitet =
                service.opprettYrkesaktivitet(
                    call.periodeReferanse(),
                    yrkesaktivitetRequest.kategorisering,
                    call.saksbehandler(),
                )
            call.respondText(
                yrkesaktivitet.tilDto().serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.Created,
            )
        }

        route("/{$PARAM_YRKESAKTIVITETUUID}") {
            put("/dagoversikt") {
                val dagerSomSkalOppdateres = call.receive<DagerSomSkalOppdateres>()
                service.oppdaterDagoversiktDager(call.yrkesaktivitetReferanse(), dagerSomSkalOppdateres, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/kategorisering") {
                val kategorisering = call.receive<YrkesaktivitetKategorisering>()
                service.oppdaterKategorisering(call.yrkesaktivitetReferanse(), kategorisering, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

data class YrkesaktivitetCreateRequest(
    val kategorisering: JsonNode,
)
