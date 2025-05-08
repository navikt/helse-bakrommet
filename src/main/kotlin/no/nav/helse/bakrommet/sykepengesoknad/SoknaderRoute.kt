package no.nav.helse.bakrommet.sykepengesoknad

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboClient
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
            val fom = LocalDate.now().minusDays(200)
            val oboToken =
                oboClient.exchangeToken(
                    bearerToken = call.request.bearerToken(),
                    scope = configuration.sykepengesoknadBackend.scope,
                )
            val soknader: List<SykepengesoknadDTO> =
                sykepengesoknadBackendClient.hentSoknader(
                    sykepengesoknadToken = oboToken,
                    fnr = fnr,
                    fom,
                )
            call.respondText(soknader.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
