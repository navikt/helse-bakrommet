package no.nav.helse.bakrommet.saksbehandlingsperiode.aareg

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.*
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.serialisertTilString
import java.util.*

internal fun Route.arbeidsforholdRelativeRoute(
    aaRegClient: AARegClient,
    personDao: PersonDao,
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    dokumentDao: DokumentDao,
) {
    route("/arbeidsforhold") {
        route("/hent") {
            post {
                fun dokumentUri(dokId: UUID): String {
                    // TODO: Er det en mer elegant måte å gjøre dette på? :
                    return call.request.uri.split("/arbeidsforhold/hent")[0].also {
                        require(it.endsWith("/dokumenter"))
                    }.let { dokUri ->
                        "$dokUri/$dokId"
                    }
                }

                call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                    val fnr = personDao.finnNaturligIdent(periode.spilleromPersonId)!!
                    logg.info("Henter aareg for periode={}", periode.id)
                    val aaregDokument: Dokument =
                        aaRegClient.hentArbeidsforholdForMedSporing(
                            fnr = fnr,
                            saksbehandlerToken = call.request.bearerToken(),
                        ).let { (arbeidsforholdRes, kildespor) ->
                            // TODO: Sjekk om akkurat samme dokument med samme innhold allerede eksisterer ?
                            dokumentDao.opprettDokument(
                                Dokument(
                                    dokumentType = DokumentType.arbeidsforhold,
                                    eksternId = null,
                                    innhold = arbeidsforholdRes.serialisertTilString(),
                                    request = kildespor,
                                    opprettetForBehandling = periode.id,
                                ),
                            )
                        }

                    call.response.headers.append(HttpHeaders.Location, dokumentUri(aaregDokument.id))
                    call.respondText(
                        aaregDokument.tilDto().serialisertTilString(),
                        ContentType.Application.Json,
                        HttpStatusCode.Created,
                    )
                }
            }
        }
    }
}
