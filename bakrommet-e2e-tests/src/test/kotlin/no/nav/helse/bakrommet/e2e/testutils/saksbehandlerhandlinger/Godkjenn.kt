package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

/**
 * Godkjenn som returnerer BehandlingDto direkte og asserter på 200 status.
 * Bruk godkjennResult hvis du trenger å håndtere feilsituasjoner.
 */
suspend fun ApplicationTestBuilder.godkjennOld(
    pseudoId: UUID,
    behandlingId: UUID,
    token: String,
): BehandlingDto {
    val result = godkjenn(pseudoId, behandlingId, token)
    check(result is ApiResult.Success) { "Godkjenning feilet" }
    return result.response
}

/**
 * Godkjenn som returnerer ApiResult for å kunne teste feilsituasjoner.
 */
suspend fun ApplicationTestBuilder.godkjenn(
    pseudoId: UUID,
    behandlingId: UUID,
    token: String,
): ApiResult<BehandlingDto> =
    client
        .post("/v1/$pseudoId/behandlinger/$behandlingId/godkjenn") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
        }.let {
            it.result<BehandlingDto> {
                assertEquals(200, it.status.value, "Godkjenning skal returnere status 200")
            }
        }
