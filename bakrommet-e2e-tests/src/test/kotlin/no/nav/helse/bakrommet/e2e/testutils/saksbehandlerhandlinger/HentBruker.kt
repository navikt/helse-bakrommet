package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Hent bruker som returnerer ApiResult for å kunne teste både success og feilsituasjoner.
 */
suspend fun ApplicationTestBuilder.hentBruker(
    token: String = TestOppsett.userToken,
): ApiResult<Bruker> =
    client
        .get("/v1/bruker") {
            bearerAuth(token)
        }.let {
            it.result<Bruker> {
                assertEquals(200, it.status.value, "Henting av bruker skal returnere status 200")
            }
        }
