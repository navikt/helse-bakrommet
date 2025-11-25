package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.TestOppsett
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

internal suspend fun ApplicationTestBuilder.slettTilkommenInntekt(
    personId: String,
    periodeId: UUID,
    tilkommenInntektId: UUID,
) {
    val response =
        client.delete("/v1/$personId/saksbehandlingsperioder/$periodeId/tilkommeninntekt/$tilkommenInntektId") {
            bearerAuth(TestOppsett.userToken)
        }
    assertEquals(204, response.status.value)
}
