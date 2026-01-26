package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

/**
 * Send tilbake som returnerer BehandlingDto direkte og asserter på 200 status.
 * Bruk sendTilbakeResult hvis du trenger å håndtere feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.sendTilbakeOld(
    personId: String,
    behandlingId: UUID,
    token: String,
    kommentar: String = "Dette blir litt feil",
): BehandlingDto {
    val result = sendTilbake(personId, behandlingId, token, kommentar)
    check(result is ApiResult.Success) { "Send tilbake feilet" }
    return result.response
}

/**
 * Send tilbake som returnerer ApiResult for å kunne teste feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.sendTilbake(
    personId: String,
    behandlingId: UUID,
    token: String,
    kommentar: String = "Dette blir litt feil",
): ApiResult<BehandlingDto> =
    client
        .post("/v1/$personId/behandlinger/$behandlingId/sendtilbake") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{ "kommentar": "$kommentar" }""")
        }.let {
            it.result<BehandlingDto> {
                assertEquals(200, it.status.value, "Send tilbake skal returnere status 200")
            }
        }

/**
 * Send tilbake med raw body - brukes for å teste malformed requests og andre edge cases.
 * Returnerer HttpResponse direkte for full kontroll over validering.
 */
internal suspend fun ApplicationTestBuilder.sendTilbakeRaw(
    personId: UUID,
    behandlingId: UUID,
    token: String,
    body: String? = null,
    setContentType: Boolean = true,
): HttpResponse =
    client.post("/v1/$personId/behandlinger/$behandlingId/sendtilbake") {
        bearerAuth(token)
        if (setContentType) {
            contentType(ContentType.Application.Json)
        }
        if (body != null) {
            setBody(body)
        }
    }
