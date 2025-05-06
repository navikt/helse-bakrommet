package no.nav.helse.bakrommet.person

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.util.bearerToken
import java.util.*

internal fun Route.personsøkRoute(
    oboClient: OboClient,
    configuration: Configuration,
    pdlClient: PdlClient,
    personDao: PersonDao,
) {
    post("/v1/personsok") {
        val ident = call.receive<JsonNode>()["ident"].asText()
        // Ident må være 11 eller 13 siffer lang
        if (ident.length != 11 && ident.length != 13) {
            throw InputValideringException("Ident må være 11 eller 13 siffer lang")
        }

        val oboToken =
            oboClient.exchangeToken(
                bearerToken = call.request.bearerToken(),
                scope = configuration.pdl.scope,
            )

        val identer = pdlClient.hentIdenterFor(pdlToken = oboToken, ident = ident)

        fun hentEllerOpprettPersonid(naturligIdent: String): String {
            personDao.finnPersonId(*identer.toTypedArray())?.let { return it }
            val newPersonId = UUID.randomUUID().toString().replace("-", "").substring(0, 5)

            // TODO naturlig ident her må være gjeldende fnr fra hentIdenter
            personDao.opprettPerson(naturligIdent, newPersonId)
            return newPersonId
        }

        call.response.headers.append("Content-Type", "application/json")
        call.respondText("""{ "personId": "${hentEllerOpprettPersonid(ident)}" }""")
    }
}
