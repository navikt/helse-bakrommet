package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.slettYrkesaktivitet(
    personId: String,
    periodeId: UUID,
    yrkesaktivitetId: UUID,
) {
    val response =
        client.delete("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId") {
            bearerAuth(TestOppsett.userToken)
        }
    assertEquals(204, response.status.value)
}
