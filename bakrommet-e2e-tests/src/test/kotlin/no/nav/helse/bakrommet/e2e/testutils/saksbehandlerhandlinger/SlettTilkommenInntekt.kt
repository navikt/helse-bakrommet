package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.e2e.TestOppsett
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

internal suspend fun ApplicationTestBuilder.slettTilkommenInntekt(
    personId: UUID,
    behandlingId: UUID,
    tilkommenInntektId: UUID,
) {
    val response =
        client.delete("/v1/$personId/behandlinger/$behandlingId/tilkommeninntekt/$tilkommenInntektId") {
            bearerAuth(TestOppsett.userToken)
        }
    assertEquals(204, response.status.value)
}
