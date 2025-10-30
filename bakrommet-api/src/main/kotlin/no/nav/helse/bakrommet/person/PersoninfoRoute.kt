package no.nav.helse.bakrommet.person

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlIdent
import no.nav.helse.bakrommet.pdl.alder
import no.nav.helse.bakrommet.pdl.formattert
import no.nav.helse.bakrommet.util.serialisertTilString

fun Route.personinfoRoute(
    pdlClient: PdlClient,
    personDao: PersonDao,
) {
    get("/v1/{$PARAM_PERSONID}/personinfo") {
        call.medIdent(personDao) { fnr, personId ->
            val token = call.request.bearerToken()
            val hentPersonInfo =
                pdlClient.hentPersonInfo(
                    saksbehandlerToken = token,
                    ident = fnr,
                )
            val identer = pdlClient.hentIdenterFor(token, fnr)

            data class PersonInfo(
                val fødselsnummer: String,
                val aktørId: String,
                val navn: String,
                val alder: Int?,
            )

            val personInfo =
                PersonInfo(
                    fødselsnummer = fnr,
                    aktørId = identer.first { it.gruppe == PdlIdent.AKTORID }.ident,
                    navn = hentPersonInfo.navn.formattert(),
                    alder = hentPersonInfo.alder(),
                )

            call.respondText(personInfo.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
