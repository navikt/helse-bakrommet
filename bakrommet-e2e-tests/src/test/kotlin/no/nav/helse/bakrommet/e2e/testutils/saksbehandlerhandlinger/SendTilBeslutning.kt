package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

/**
 * Send til beslutning som returnerer BehandlingDto direkte og asserter på 200 status.
 * Bruk sendTilBeslutningResult hvis du trenger å håndtere feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.sendTilBeslutningOld(
    personId: UUID,
    behandlingId: UUID,
    token: String = TestOppsett.userToken,
    individuellBegrunnelse: String = "En begrunnelse",
): BehandlingDto {
    val result = sendTilBeslutning(personId, behandlingId, token, individuellBegrunnelse)
    check(result is ApiResult.Success) { "Send til beslutning feilet" }
    return result.response
}

/**
 * Send til beslutning som returnerer ApiResult for å kunne teste feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.sendTilBeslutning(
    personId: UUID,
    behandlingId: UUID,
    token: String = TestOppsett.userToken,
    individuellBegrunnelse: String = "En begrunnelse",
): ApiResult<BehandlingDto> =
    client
        .post("/v1/$personId/behandlinger/$behandlingId/sendtilbeslutning") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{ "individuellBegrunnelse" : "$individuellBegrunnelse" }""")
        }.let {
            it.result<BehandlingDto> {
                assertEquals(200, it.status.value, "Send til beslutning skal returnere status 200")
            }
        }
