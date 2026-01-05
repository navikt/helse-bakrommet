package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.sendTilBeslutning(
    personId: UUID,
    behandlingId: UUID,
    token: String = TestOppsett.userToken,
    individuellBegrunnelse: String = "En begrunnelse",
): BehandlingDto {
    val response =
        client.post("/v1/$personId/behandlinger/$behandlingId/sendtilbeslutning") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{ "individuellBegrunnelse" : "$individuellBegrunnelse" }""")
        }

    assertEquals(200, response.status.value, "Send til beslutning skal returnere status 200")

    return response.body<BehandlingDto>()
}
