package no.nav.helse.bakrommet.saksbehandlingsperiode.pensjonsgivendeinntekt

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.*
import no.nav.helse.bakrommet.sigrun.*
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.toJsonNode

private data class PensjonsgivendeInntektHentRequest(
    val senesteÅrTom: Int,
    val antallÅrBakover: Int,
)

internal fun Route.pensjonsgivendeInntektRelativeRoute(
    sigrunClient: SigrunClient,
    personDao: PersonDao,
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    dokumentDao: DokumentDao,
) {
    route("/pensjonsgivendeinntekt") {
        route("/hent") {
            post {
                call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                    val request = call.receive<PensjonsgivendeInntektHentRequest>()
                    val fnr = personDao.finnNaturligIdent(periode.spilleromPersonId)!!
                    logg.info("Henter pensjonsgivendeinntekt for periode={}", periode.id)

                    val pensjonsgivendeinntektDokument =
                        sigrunClient.hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(
                            fnr = fnr,
                            senesteÅrTom = request.senesteÅrTom,
                            antallÅrBakover = request.antallÅrBakover,
                            saksbehandlerToken = call.request.bearerToken(),
                        ).let { reponsMedSporing ->
                            reponsMedSporing.joinSigrunResponserTilEttDokument().let { (innhold, kildespor) ->
                                dokumentDao.opprettDokument(
                                    Dokument(
                                        dokumentType = DokumentType.pensjonsgivendeinntekt,
                                        eksternId = null,
                                        innhold = innhold.serialisertTilString(),
                                        request = kildespor,
                                        opprettetForBehandling = periode.id,
                                    ),
                                )
                            }
                        }

                    call.response.headers.append(HttpHeaders.Location, dokumentUriFor(pensjonsgivendeinntektDokument))
                    call.respondText(
                        pensjonsgivendeinntektDokument.tilDto().serialisertTilString(),
                        ContentType.Application.Json,
                        HttpStatusCode.Created,
                    )
                }
            }
        }
    }
}

fun List<PensjonsgivendeInntektÅrMedSporing>.joinSigrunResponserTilEttDokument(): Pair<JsonNode, Kildespor> {
    require(this.isNotEmpty())
    val data = map { it.data() }
    val sporingMap = mapOf(*map { it.data().inntektsaar() to it.sporing().kilde }.toTypedArray())
    return data.toJsonNode() to Kildespor(sporingMap.toString())
}
