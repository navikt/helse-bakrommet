package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.BeregningResponseUtDto
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.hentUtbetalingsberegning(
    personId: String,
    periodeId: UUID,
): BeregningResponseUtDto? {
    val response =
        client.get("/v1/$personId/saksbehandlingsperioder/$periodeId/utbetalingsberegning") {
            bearerAuth(TestOppsett.userToken)
        }
    assertEquals(200, response.status.value)
    val responseText = response.bodyAsText()
    if (responseText == "null") return null

    // API-et sender BeregningResponseUtDto (med InntektDto som har alle felt)
    return objectMapper.readValue(responseText, BeregningResponseUtDto::class.java)
}
