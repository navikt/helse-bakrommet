package no.nav.helse.bakrommet.api.person

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.auth.bearerToken
import no.nav.helse.bakrommet.api.naturligIdent
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.person.PersonService

fun Route.personinfoRoute(
    personService: PersonService,
) {
    get("/v1/{$PARAM_PSEUDO_ID}/personinfo") {
        val token = call.request.bearerToken()
        val personInfo =
            personService.hentPersonInfo(
                naturligIdent = call.naturligIdent(personService),
                saksbehandlerToken = token,
            )
        call.respondJson(personInfo.tilPersoninfoResponseDto())
    }
}
