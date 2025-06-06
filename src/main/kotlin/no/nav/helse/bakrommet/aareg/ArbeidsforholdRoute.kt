package no.nav.helse.bakrommet.aareg

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.arbeidsforholdRoute(
    aaRegClient: AARegClient,
    personDao: PersonDao,
) {
    get("/v1/{personId}/arbeidsforhold") {
        call.medIdent(personDao) { fnr, personId ->
            val arbeidsforhold: JsonNode =
                aaRegClient.hentArbeidsforholdFor(
                    fnr = fnr,
                    saksbehandlerToken = call.request.bearerToken(),
                )
            call.respondText(arbeidsforhold.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
