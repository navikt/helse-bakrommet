package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingDto
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.hentTidslinje(
    personId: UUID,
): List<TidslinjeBehandlingDto> {
    val response =
        client.get("/v2/$personId/tidslinje") {
            bearerAuth(TestOppsett.userToken)
        }

    assertEquals(200, response.status.value, "Henting av tidslinje skal returnere 200")
    return objectMapper.readValue(response.bodyAsText())
}
