package no.nav.helse.bakrommet.saksbehandlingsperiode.ainntekt

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.*
import no.nav.helse.bakrommet.saksbehandlingsperiode.medBehandlingsperiode
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.YearMonth
import java.util.UUID

data class AInntektHentRequest(
    val fom: YearMonth,
    val tom: YearMonth,
)

internal fun Route.ainntektRelativeRoute(
    aInntektClient: AInntektClient,
    personDao: PersonDao,
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    dokumentDao: DokumentDao,
) {
    route("/ainntekt") {
        route("/hent") {
            post {
                fun dokumentUri(dokId: UUID): String {
                    // TODO: Er det en mer elegant måte å gjøre dette på? :
                    return call.request.uri.split("/ainntekt/hent")[0].also {
                        require(it.endsWith("/dokumenter"))
                    }.let { dokUri ->
                        "$dokUri/$dokId"
                    }
                }

                call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                    val request = call.receive<AInntektHentRequest>()
                    val fnr = personDao.finnNaturligIdent(periode.spilleromPersonId)!!
                    logg.info("Henter inntekter for periode={}", periode.id)
                    val inntektDokument: Dokument =
                        aInntektClient.hentInntekterForMedSporing(
                            fnr = fnr,
                            maanedFom = request.fom,
                            maanedTom = request.tom,
                            saksbehandlerToken = call.request.bearerToken(),
                        ).let { (inntekter, kildespor) ->
                            // TODO: Sjekk om akkurat samme dokument med samme innhold allerede eksisterer ?
                            dokumentDao.opprettDokument(
                                Dokument(
                                    dokumentType = DokumentType.aInntekt828,
                                    eksternId = null,
                                    innhold = inntekter.serialisertTilString(),
                                    request = kildespor,
                                    opprettetForBehandling = periode.id,
                                ),
                            )
                        }

                    call.response.headers.append(HttpHeaders.Location, dokumentUri(inntektDokument.id))
                    call.respondText(inntektDokument.tilDto().serialisertTilString(), ContentType.Application.Json, HttpStatusCode.Created)
                }
            }
        }
    }
}
