package no.nav.helse.bakrommet.person

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger
import java.lang.RuntimeException
import java.sql.SQLException

internal fun Route.personsøkRoute(
    pdlClient: PdlClient,
    personDao: PersonDao,
    service: PersonsøkService = PersonsøkService(pdlClient, personDao),
) {
    post("/v1/personsok") {
        val ident = call.receive<JsonNode>()["ident"].asText()
        val newPersonId = service.hentEllerOpprettPersonid(ident, call.saksbehandlerOgToken())
        call.response.headers.append("Content-Type", "application/json")
        call.respondText("""{ "personId": "$newPersonId" }""")
    }
}

class PersonsøkService(
    private val pdlClient: PdlClient,
    private val personDao: PersonDao,
    private val personIdFactory: PersonIdFactory = SpilleromPersonId,
) {
    suspend fun hentEllerOpprettPersonid(
        ident: String,
        saksbehandler: BrukerOgToken,
    ): SpilleromPersonId {
        // Ident må være 11 eller 13 siffer lang
        if (ident.length != 11 && ident.length != 13) {
            throw InputValideringException("Ident må være 11 eller 13 siffer lang")
        }
        val identer = pdlClient.hentIdenterFor(saksbehandlerToken = saksbehandler.token, ident = ident)
        personDao.finnPersonId(*identer.toTypedArray())?.let { return SpilleromPersonId(it) }

        // TODO naturlig ident her må være gjeldende fnr fra hentIdenter
        val newPersonId =
            prøvOpprettFor(naturligIdent = ident)
                ?: prøvOpprettFor(naturligIdent = ident)
                ?: prøvOpprettFor(naturligIdent = ident)
                ?: prøvOpprettFor(naturligIdent = ident)
                ?: throw RuntimeException("Klarte ikke opprette SpilleromPersonId")

        return newPersonId
    }

    private fun prøvOpprettFor(naturligIdent: String): SpilleromPersonId? {
        val newPersonId = personIdFactory.lagNy()
        try {
            personDao.opprettPerson(naturligIdent = naturligIdent, newPersonId.personId)
        } catch (ex: SQLException) {
            val basisTekst = "SQLException ved opprettelese av ny SpilleromPersonId. Antakeligvis kollisjon."
            logg.error("$basisTekst, se sikkerlogg")
            sikkerLogger.error("$basisTekst, personId=${newPersonId.personId}, ident[0:6]=${naturligIdent.take(6)}", ex)
            return null
        }
        return newPersonId
    }
}
