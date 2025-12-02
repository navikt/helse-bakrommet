package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingDto
import no.nav.helse.bakrommet.tidslinje.Tidslinje
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals

internal suspend fun ApplicationTestBuilder.hentTidslinje(
    personId: String,
): List<no.nav.helse.bakrommet.tidslinje.TidslinjeRad> {
    val response =
        client.get("/v1/$personId/tidslinje") {
            bearerAuth(TestOppsett.userToken)
        }

    assertEquals(200, response.status.value, "Henting av tidslinje skal returnere 200")
    val tidslinje: Tidslinje = objectMapper.readValue(response.bodyAsText())
    return tidslinje.rader
}

internal suspend fun ApplicationTestBuilder.hentTidslinjeV2(
    personId: String,
): List<TidslinjeBehandlingDto> {
    val response =
        client.get("/v2/$personId/tidslinje") {
            bearerAuth(TestOppsett.userToken)
        }

    assertEquals(200, response.status.value, "Henting av tidslinje skal returnere 200")
    return objectMapper.readValue(response.bodyAsText())
}
