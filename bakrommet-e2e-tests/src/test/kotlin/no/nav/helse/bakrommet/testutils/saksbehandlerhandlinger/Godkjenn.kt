package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.behandling.Saksbehandlingsperiode
import org.junit.jupiter.api.Assertions.assertEquals

suspend fun ApplicationTestBuilder.godkjenn(
    saksbehandlingsperiode: Saksbehandlingsperiode,
    token: String,
) {
    val response =
        this.client.post("/v1/${saksbehandlingsperiode.spilleromPersonId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/godkjenn") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
        }
    assertEquals(200, response.status.value)
}
