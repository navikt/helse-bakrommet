package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.e2e.TestOppsett
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.slettYrkesaktivitet(
    pseudoId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
) {
    val response =
        client.delete("/v1/$pseudoId/behandlinger/$behandlingId/yrkesaktivitet/$yrkesaktivitetId") {
            bearerAuth(TestOppsett.userToken)
        }
    assertEquals(204, response.status.value)
}
