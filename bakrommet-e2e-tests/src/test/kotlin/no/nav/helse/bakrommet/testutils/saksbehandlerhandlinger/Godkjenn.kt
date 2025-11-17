package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.behandling.Behandling
import org.junit.jupiter.api.Assertions.assertEquals

suspend fun ApplicationTestBuilder.godkjenn(
    behandling: Behandling,
    token: String,
) {
    val response =
        this.client.post("/v1/${behandling.spilleromPersonId}/saksbehandlingsperioder/${behandling.id}/godkjenn") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
        }
    assertEquals(200, response.status.value)
}
