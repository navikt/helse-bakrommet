package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

suspend fun ApplicationTestBuilder.revurder(
    pseudoId: UUID,
    behandlingId: UUID,
    token: String = TestOppsett.userToken,
): BehandlingDto {
    val response =
        this.client.post(
            "/v1/$pseudoId/behandlinger/$behandlingId/revurder",
        ) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
        }
    assertEquals(201, response.status.value)
    return objectMapper.readValue(response.bodyAsText())
}
