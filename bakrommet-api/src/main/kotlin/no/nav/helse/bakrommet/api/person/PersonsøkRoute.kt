package no.nav.helse.bakrommet.api.person

import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.dto.person.PersonsøkRequestDto
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.person.PersonsøkService
import java.lang.IllegalArgumentException

fun PersonsøkRequestDto.naturligIdent(): NaturligIdent =
    try {
        NaturligIdent(ident)
    } catch (iae: IllegalArgumentException) {
        throw InputValideringException(iae.message ?: "Klarte ikke parse naturlig ident")
    }

fun Route.personsøkRoute(
    service: PersonsøkService,
) {
    post("/v1/personsok") {
        val request = call.receive<PersonsøkRequestDto>()
        val newPersonId = service.hentEllerOpprettPseudoId(request.naturligIdent())
        call.respondJson(newPersonId.tilPersonsøkResponseDto())
    }
}
