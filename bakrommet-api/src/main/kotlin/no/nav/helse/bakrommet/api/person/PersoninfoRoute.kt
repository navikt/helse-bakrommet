package no.nav.helse.bakrommet.api.person

import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.person.SpilleromPersonId

fun Route.personinfoRoute(
    personService: PersonService,
) {
    get("/v1/{$PARAM_PERSONID}/personinfo") {
        val personId =
            call.parameters[PARAM_PERSONID]
                ?: throw IllegalArgumentException("Mangler personId i path")
        val token = call.request.bearerToken()
        val personInfo =
            personService.hentPersonInfo(
                spilleromPersonId = SpilleromPersonId(personId),
                saksbehandlerToken = token,
            )
        call.respondJson(personInfo.tilPersoninfoResponseDto())
    }
}
