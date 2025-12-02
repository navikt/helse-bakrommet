package no.nav.helse.bakrommet.api.organisasjon

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.organisasjon.OrganisasjonService

fun Route.organisasjonRoute(service: OrganisasjonService) {
    get("/v1/organisasjon/{orgnummer}") {
        val orgnummer = call.parameters["orgnummer"] ?: throw InputValideringException("Mangler orgnummer i path")
        val organisasjon = service.hentOrganisasjonsnavn(orgnummer)
        call.respondText(organisasjon.navn, ContentType.Text.Plain, HttpStatusCode.OK)
    }
}
