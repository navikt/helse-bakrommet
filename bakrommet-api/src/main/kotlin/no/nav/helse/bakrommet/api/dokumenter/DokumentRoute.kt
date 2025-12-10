package no.nav.helse.bakrommet.api.dokumenter

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.api.PARAM_BEHANDLING_ID
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.periodeReferanse
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.util.somGyldigUUID

fun RoutingContext.dokumentUriFor(dokument: Dokument): String {
    val periodeId = call.parameters[PARAM_BEHANDLING_ID].somGyldigUUID()
    val personId = call.parameters[PARAM_PSEUDO_ID]!!
    val dokUri = "/v1/$personId/behandlinger/$periodeId/dokumenter"
    check(call.request.uri.startsWith(dokUri)) {
        "Forventet å være i kontekst av /dokumenter for å kunne resolve dokument-uri"
    }
    return "$dokUri/${dokument.id}"
}

private suspend fun RoutingCall.respondDokument(
    dokument: Dokument,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondJson(dokument.tilDokumentDto(), status)
}

fun Route.dokumentRoute(
    dokumentHenter: DokumentHenter,
    personService: PersonService,
) {
    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/dokumenter") {
        get {
            val dokumenterDto = dokumentHenter.hentDokumenterFor(call.periodeReferanse(personService)).map { it.tilDokumentDto() }
            call.respondJson(dokumenterDto)
        }

        route("/{dokumentUUID}") {
            get {
                val ref = call.periodeReferanse(personService)
                val dokumentId = call.parameters["dokumentUUID"].somGyldigUUID()
                val dok = dokumentHenter.hentDokument(ref, dokumentId)
                if (dok == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respondDokument(dok)
                }
            }
        }

        route("/ainntekt") {
            route("/hent-8-28") {
                post {
                    val inntektDokument =
                        dokumentHenter.hentOgLagreAInntekt828(
                            call.periodeReferanse(personService),
                            call.saksbehandlerOgToken(),
                        )
                    call.response.headers.append(HttpHeaders.Location, dokumentUriFor(inntektDokument))
                    call.respondDokument(inntektDokument, HttpStatusCode.Created)
                }
            }
            route("/hent-8-30") {
                post {
                    val inntektDokument =
                        dokumentHenter.hentOgLagreAInntekt830(
                            call.periodeReferanse(personService),
                            call.saksbehandlerOgToken(),
                        )
                    call.response.headers.append(HttpHeaders.Location, dokumentUriFor(inntektDokument))
                    call.respondDokument(inntektDokument, HttpStatusCode.Created)
                }
            }
        }

        route("/arbeidsforhold") {
            route("/hent") {
                post {
                    val aaregDokument =
                        dokumentHenter.hentOgLagreArbeidsforhold(
                            ref = call.periodeReferanse(personService),
                            saksbehandler = call.saksbehandlerOgToken(),
                        )
                    call.response.headers.append(HttpHeaders.Location, dokumentUriFor(aaregDokument))
                    call.respondDokument(aaregDokument, HttpStatusCode.Created)
                }
            }
        }

        route("/pensjonsgivendeinntekt") {
            route("/hent") {
                post {
                    val pensjonsgivendeinntektDokument =
                        dokumentHenter.hentOgLagrePensjonsgivendeInntekt(
                            ref = call.periodeReferanse(personService),
                            saksbehandler = call.saksbehandlerOgToken(),
                        )
                    call.response.headers.append(HttpHeaders.Location, dokumentUriFor(pensjonsgivendeinntektDokument))
                    call.respondDokument(pensjonsgivendeinntektDokument, HttpStatusCode.Created)
                }
            }
        }
    }
}
