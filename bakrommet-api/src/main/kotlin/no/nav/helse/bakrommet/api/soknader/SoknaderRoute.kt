package no.nav.helse.bakrommet.api.soknader

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.personId
import no.nav.helse.bakrommet.sykepengesoknad.SoknaderService
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.LocalDate

fun Route.soknaderRoute(service: SoknaderService) {
    route("/v1/{$PARAM_PERSONID}/soknader") {
        get {
            val personId = call.personId()
            val fomParam = call.request.queryParameters["fom"]
            val fom =
                fomParam?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        throw InputValideringException("Ugyldig 'fom'-parameter. Forventet format: yyyy-MM-dd")
                    }
                } ?: LocalDate.now().minusYears(1)

            val medSporsmal = call.request.queryParameters["medSporsmal"]?.toBoolean() ?: false

            val soknader =
                service.hentSoknader(
                    saksbehandlerToken = call.request.bearerToken(),
                    personId = personId,
                    fom = fom,
                    medSporsmal = medSporsmal,
                )
            call.respondText(soknader.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }

    route("/v1/{$PARAM_PERSONID}/soknader/{soknadId}") {
        get {
            val personId = call.personId()
            val soknadId = call.parameters["soknadId"] ?: throw InputValideringException("Mangler søknadId")

            val soknad =
                service.hentSoknad(
                    saksbehandlerToken = call.request.bearerToken(),
                    personId = personId,
                    soknadId = soknadId,
                )
            call.respondText(soknad.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
