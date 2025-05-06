package no.nav.helse.bakrommet.person

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.alder
import no.nav.helse.bakrommet.pdl.formattert
import no.nav.helse.bakrommet.util.bearerToken
import no.nav.helse.bakrommet.util.serialisertTilString
import java.util.*

internal fun Route.personinfoRoute(
    oboClient: OboClient,
    configuration: Configuration,
    pdlClient: PdlClient,
    personDao: PersonDao,
) {
    get("/v1/{personId}/personinfo") {
        call.medIdent(personDao) { fnr, personId ->
            val oboToken =
                oboClient.exchangeToken(
                    bearerToken = call.request.bearerToken(),
                    scope = configuration.pdl.scope,
                )

            val hentPersonInfo =
                pdlClient.hentPersonInfo(
                    pdlToken = oboToken,
                    ident = fnr,
                )
            val identer = pdlClient.hentIdenterFor(oboToken, fnr)

            data class PersonInfo(
                val fødselsnummer: String,
                val aktørId: String,
                val navn: String,
                val alder: Int?,
            )

            val personInfo =
                PersonInfo(
                    fødselsnummer = fnr,
                    aktørId = identer.first { it.length == 13 },
                    navn = hentPersonInfo.navn.formattert(),
                    alder = hentPersonInfo.alder(),
                )

            call.respondText(personInfo.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}
