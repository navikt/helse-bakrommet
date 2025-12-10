package no.nav.helse.bakrommet.api.person

import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.PARAM_PERSONID
import no.nav.helse.bakrommet.api.naturligIdent
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.person.PersonService

fun Route.personinfoRoute(
    personService: PersonService,
) {
    get("/v1/{$PARAM_PERSONID}/personinfo") {
        val token = call.request.bearerToken()
        val personInfo =
            personService.hentPersonInfo(
                naturligIdent = call.naturligIdent(personService),
                saksbehandlerToken = token,
            )
        call.respondJson(personInfo.tilPersoninfoResponseDto())
    }
}
