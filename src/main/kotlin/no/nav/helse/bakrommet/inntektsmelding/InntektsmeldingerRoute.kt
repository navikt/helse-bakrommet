package no.nav.helse.bakrommet.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.util.bearerToken
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.LocalDate

internal fun Route.inntektsmeldingerRoute(
    oboClient: OboClient,
    configuration: Configuration,
    inntektsmeldingClient: InntektsmeldingClient,
    personDao: PersonDao,
) {
    get("/v1/{personId}/inntektsmeldinger") {
        call.medIdent(personDao) { fnr, personId ->
            fun parseLocalDateParam(paramName: String) =
                call.request.queryParameters[paramName].let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        throw InputValideringException("Ugyldig '$paramName'-parameter. Forventet format: yyyy-MM-dd")
                    }
                }

            val fom = parseLocalDateParam("fom")
            val tom = parseLocalDateParam("tom")

            val oboToken = call.request.bearerToken().exchangeWithObo(oboClient, configuration.inntektsmelding.scope)
            val inntektsmeldinger: JsonNode =
                inntektsmeldingClient.hentInntektsmeldinger(
                    fnr = fnr,
                    fom = fom,
                    tom = tom,
                    inntektsmeldingToken = oboToken,
                )
            call.respondText(inntektsmeldinger.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
