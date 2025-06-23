package no.nav.helse.bakrommet.sigrun

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.pensjonsgivendeInntektRoute(
    sigrunClient: SigrunClient,
    personDao: PersonDao,
) {
    get("/v1/{personId}/pensjonsgivendeinntekt") {
        call.medIdent(personDao) { fnr, personId ->
            fun parseYearParam(paramName: String): Int =
                call.request.queryParameters[paramName].let { årParam ->
                    try {
                        requireNotNull(årParam)
                        årParam.toInt()
                        // TODO: Godta kun fornuftig range ?
                    } catch (e: Exception) {
                        throw InputValideringException("Ugyldig '$paramName'-parameter. Forventet er gyldig årstall")
                    }
                }

            val inntektsaar = parseYearParam("inntektsaar") // TODO: Flere år på en gang ?
            val pensjonsgivendeInntekt: JsonNode =
                sigrunClient.hentPensjonsgivendeInntekt(
                    fnr = fnr,
                    inntektsAar = inntektsaar,
                    saksbehandlerToken = call.request.bearerToken(),
                )
            call.respondText(
                pensjonsgivendeInntekt.serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }
    }
}
