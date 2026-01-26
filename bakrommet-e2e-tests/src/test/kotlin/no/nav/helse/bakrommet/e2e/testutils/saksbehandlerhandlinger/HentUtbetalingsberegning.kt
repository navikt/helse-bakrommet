package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.BeregningResponseDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

internal suspend fun ApplicationTestBuilder.hentUtbetalingsberegning(
    personId: UUID,
    behandlingId: UUID,
): BeregningResponseDto? {
    val response =
        client.get("/v1/$personId/behandlinger/$behandlingId/utbetalingsberegning") {
            bearerAuth(TestOppsett.userToken)
        }
    assertEquals(200, response.status.value)
    return response.body()
}
