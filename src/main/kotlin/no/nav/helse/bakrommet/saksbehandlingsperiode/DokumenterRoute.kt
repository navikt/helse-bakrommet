package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.time.YearMonth

data class AInntektHentRequest(
    val fom: YearMonth,
    val tom: YearMonth,
)

internal fun Route.dokumenterRoute(
    dokumentHenter: DokumentHenter,
    dokumentRoutes: List<Route.() -> Unit> = emptyList(),
) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/dokumenter") {
        get {
            val dokumenterDto = dokumentHenter.hentDokumenterFor(call.periodeReferanse()).map { it.tilDto() }
            call.respondText(dokumenterDto.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }

        route("/{dokumentUUID}") {
            get {
                val ref = call.periodeReferanse()
                val dokumentId = call.parameters["dokumentUUID"].somGyldigUUID()
                val dok = dokumentHenter.hentDokument(ref, dokumentId)
                if (dok == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respondText(
                        dok.tilDto().serialisertTilString(),
                        ContentType.Application.Json,
                        HttpStatusCode.OK,
                    )
                }
            }
        }

        route("/ainntekt") {
            route("/hent") {
                post {
                    val request = call.receive<AInntektHentRequest>()
                    val inntektDokument =
                        dokumentHenter.hentOgLagreAInntekt(
                            call.periodeReferanse(),
                            request.fom,
                            request.tom,
                            call.saksbehandlerOgToken(),
                        )
                    call.response.headers.append(HttpHeaders.Location, dokumentUriFor(inntektDokument))
                    call.respondText(
                        inntektDokument.tilDto().serialisertTilString(),
                        ContentType.Application.Json,
                        HttpStatusCode.Created,
                    )
                }
            }
        }

        dokumentRoutes.forEach { dokRoute ->
            dokRoute(this)
        }
    }
}
