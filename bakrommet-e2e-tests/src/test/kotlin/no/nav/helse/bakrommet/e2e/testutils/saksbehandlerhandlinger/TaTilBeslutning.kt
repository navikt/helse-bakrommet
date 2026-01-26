package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

/**
 * Ta til beslutning som returnerer BehandlingDto direkte og asserter på 200 status.
 * Bruk taTilBeslutningResult hvis du trenger å håndtere feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.taTilBeslutningOld(
    personId: UUID,
    behandlingId: UUID,
    token: String,
): BehandlingDto {
    val result = taTilBeslutning(personId, behandlingId, token)
    check(result is ApiResult.Success) { "Ta til beslutning feilet" }
    return result.response
}

/**
 * Ta til beslutning som returnerer ApiResult for å kunne teste feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.taTilBeslutning(
    personId: UUID,
    behandlingId: UUID,
    token: String,
): ApiResult<BehandlingDto> =
    client
        .post("/v1/$personId/behandlinger/$behandlingId/tatilbeslutning") {
            bearerAuth(token)
        }.let {
            it.result<BehandlingDto> {
                assertEquals(200, it.status.value, "Ta til beslutning skal returnere status 200")
            }
        }
