package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

suspend fun ApplicationTestBuilder.godkjenn(
    pseudoId: UUID,
    behandlingId: UUID,
    token: String,
) {
    val response =
        this.client.post("/v1/$pseudoId/behandlinger/$behandlingId/godkjenn") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
        }
    assertEquals(200, response.status.value)
}
