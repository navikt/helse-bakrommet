package no.nav.helse.bakrommet.api.person

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.api.dto.person.PersonsøkRequestDto
import no.nav.helse.bakrommet.api.errorhandling.respondProblem
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.PersonIkkeFunnetException
import no.nav.helse.bakrommet.errorhandling.ProblemDetails
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
        val ident = request.naturligIdent()
        runCatching {
            service.hentIdenter(ident, call.saksbehandlerOgToken())
        }.onFailure {
            if (it is PersonIkkeFunnetException) {
                val problemDetails =
                    ProblemDetails(
                        status = 404,
                        title = "Person ikke funnet",
                        detail = "Fant ikke person i PDL",
                    )
                call.respondProblem(HttpStatusCode.NotFound, problemDetails)
            } else {
                throw it
            }
        }
        val newPersonId = service.hentEllerOpprettPseudoId(ident)
        call.respondJson(newPersonId.tilPersonsøkResponseDto())
    }
}
