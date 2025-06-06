package no.nav.helse.bakrommet.aareg

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.util.bearerToken
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.arbeidsforholdRoute(
    oboClient: OboClient,
    configuration: Configuration,
    aaRegClient: AARegClient,
    personDao: PersonDao,
) {
    get("/v1/{personId}/arbeidsforhold") {
        call.medIdent(personDao) { fnr, personId ->
            val oboToken = call.request.bearerToken().exchangeWithObo(oboClient, configuration.aareg.scope)
            val arbeidsforhold: JsonNode =
                aaRegClient.hentArbeidsforholdFor(
                    fnr = fnr,
                    aaregToken = oboToken,
                )
            call.respondText(arbeidsforhold.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
