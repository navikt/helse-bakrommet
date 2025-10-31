package no.nav.helse.bakrommet.sykepengesoknad

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.person.PersonIdService
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

internal fun Route.soknaderRoute(
    sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    personIdService: PersonIdService,
) {
    get("/v1/{$PARAM_PERSONID}/soknader") {
        call.medIdent(personIdService) { fnr, personId ->
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

            val soknader: List<SykepengesoknadDTO> =
                sykepengesoknadBackendClient.hentSoknader(
                    saksbehandlerToken = call.request.bearerToken(),
                    fnr = fnr,
                    fom,
                    medSporsmal = medSporsmal,
                )
            call.respondText(soknader.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }

    get("/v1/{$PARAM_PERSONID}/soknader/{soknadId}") {
        call.medIdent(personIdService) { fnr, personId ->
            val soknadId = call.parameters["soknadId"] ?: throw InputValideringException("Mangler søknadId")

            val soknad: SykepengesoknadDTO =
                sykepengesoknadBackendClient.hentSoknad(
                    saksbehandlerToken = call.request.bearerToken(),
                    id = soknadId,
                )
            call.respondText(soknad.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
