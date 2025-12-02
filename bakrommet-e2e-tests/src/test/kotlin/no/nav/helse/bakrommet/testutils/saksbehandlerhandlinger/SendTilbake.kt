package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.behandling.Behandling
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.sendTilbake(
    personId: String,
    periodeId: UUID,
    token: String,
    kommentar: String = "Dette blir litt feil",
): Behandling {
    val response =
        client.post("/v1/$personId/behandlinger/$periodeId/sendtilbake") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{ "kommentar": "$kommentar" }""")
        }

    assertEquals(200, response.status.value, "Send tilbake skal returnere status 200")

    return response.body<Behandling>()
}
