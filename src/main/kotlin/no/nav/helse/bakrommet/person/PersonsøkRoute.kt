package no.nav.helse.bakrommet.person

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.pdl.PdlClient
import java.util.*

internal fun Route.personsøkRoute(
    pdlClient: PdlClient,
    personDao: PersonDao,
) {
    post("/v1/personsok") {
        val ident = call.receive<JsonNode>()["ident"].asText()
        // Ident må være 11 eller 13 siffer lang
        if (ident.length != 11 && ident.length != 13) {
            throw InputValideringException("Ident må være 11 eller 13 siffer lang")
        }

        val identer = pdlClient.hentIdenterFor(saksbehandlerToken = call.request.bearerToken(), ident = ident)

        fun hentEllerOpprettPersonid(naturligIdent: String): SpilleromPersonId {
            personDao.finnPersonId(*identer.toTypedArray())?.let { return SpilleromPersonId(it) }
            val newPersonId = SpilleromPersonId.lagNy()

            // TODO naturlig ident her må være gjeldende fnr fra hentIdenter
            personDao.opprettPerson(naturligIdent, newPersonId.personId)
            return newPersonId
        }

        call.response.headers.append("Content-Type", "application/json")
        call.respondText("""{ "personId": "${hentEllerOpprettPersonid(ident)}" }""")
    }
}
