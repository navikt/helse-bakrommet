package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.errorhandling.ProblemDetails

/**
 * Hent organisasjonsnavn som returnerer ApiResult for å kunne teste både success og feilsituasjoner.
 */
suspend fun ApplicationTestBuilder.hentOrganisasjonsnavn(
    orgnummer: String,
    token: String = TestOppsett.userToken,
): ApiResult<String> =
    client
        .get("/v1/organisasjon/$orgnummer") {
            bearerAuth(token)
        }.let { response ->
            if (response.status.value in 200..299) {
                ApiResult.Success(response.bodyAsText())
            } else {
                ApiResult.Error(response.body<ProblemDetails>())
            }
        }
