package no.nav.helse.bakrommet.sykepengesoknad

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
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

internal fun Route.soknaderRoute(
    oboClient: OboClient,
    configuration: Configuration,
    sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    personDao: PersonDao,
) {
    get("/v1/{personId}/soknader") {
        call.medIdent(personDao) { fnr, personId ->
            val fomParam = call.request.queryParameters["fom"]
            val fom =
                fomParam?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        throw InputValideringException("Ugyldig 'fom'-parameter. Forventet format: yyyy-MM-dd")
                    }
                } ?: LocalDate.now().minusYears(1)

            val oboToken = call.request.bearerToken().exchangeWithObo(oboClient, configuration.sykepengesoknadBackend.scope)
            val soknader: List<SykepengesoknadDTO> =
                sykepengesoknadBackendClient.hentSoknader(
                    sykepengesoknadToken = oboToken,
                    fnr = fnr,
                    fom,
                )
            call.respondText(soknader.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }

    get("/v1/{personId}/soknader/{soknadId}") {
        call.medIdent(personDao) { fnr, personId ->
            val soknadId = call.parameters["soknadId"] ?: throw InputValideringException("Mangler søknadId")

            val oboToken = call.request.bearerToken().exchangeWithObo(oboClient, configuration.sykepengesoknadBackend.scope)
            val soknad: SykepengesoknadDTO =
                sykepengesoknadBackendClient.hentSoknad(
                    sykepengesoknadToken = oboToken,
                    id = soknadId,
                )
            call.respondText(soknad.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
