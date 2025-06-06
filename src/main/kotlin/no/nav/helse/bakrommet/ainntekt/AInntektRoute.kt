package no.nav.helse.bakrommet.ainntekt

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
import java.time.YearMonth

internal fun Route.ainntektRoute(
    oboClient: OboClient,
    configuration: Configuration,
    aInntektClient: AInntektClient,
    personDao: PersonDao,
) {
    get("/v1/{personId}/ainntekt") {
        call.medIdent(personDao) { fnr, personId ->
            fun parseYearMonthParam(paramName: String) =
                call.request.queryParameters[paramName].let {
                    try {
                        YearMonth.parse(it)
                    } catch (e: Exception) {
                        throw InputValideringException("Ugyldig '$paramName'-parameter. Forventet format: yyyy-MM")
                    }
                }

            val fom = parseYearMonthParam("fom")
            val tom = parseYearMonthParam("tom")

            val oboToken = call.request.bearerToken().exchangeWithObo(oboClient, configuration.ainntekt.scope)
            val inntekter: JsonNode =
                aInntektClient.hentInntekterFor(
                    fnr = fnr,
                    maanedFom = fom,
                    maanedTom = tom,
                    ainntektToken = oboToken,
                )
            call.respondText(inntekter.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
