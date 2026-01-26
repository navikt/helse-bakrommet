package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndring
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.somListe
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

/**
 * Henter historikk for en behandling.
 */
suspend fun ApplicationTestBuilder.hentHistorikk(
    personPseudoId: UUID,
    behandlingId: UUID,
    token: String = TestOppsett.userToken,
): List<SaksbehandlingsperiodeEndring> {
    val response =
        client.get("/v1/$personPseudoId/behandlinger/$behandlingId/historikk") {
            bearerAuth(token)
        }
    assertEquals(200, response.status.value, "Henting av historikk skal returnere status 200")
    return response.bodyAsText().somListe<SaksbehandlingsperiodeEndring>()
}
