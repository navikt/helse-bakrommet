package no.nav.helse.bakrommet.api.person

import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.dto.person.PersonsøkRequestDto
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.person.PersonsøkService

fun Route.personsøkRoute(
    service: PersonsøkService,
) {
    post("/v1/personsok") {
        val request = call.receive<PersonsøkRequestDto>()
        val newPersonId = service.hentEllerOpprettPseudoId(NaturligIdent(request.ident))
        call.respondJson(newPersonId.tilPersonsøkResponseDto())
    }
}
