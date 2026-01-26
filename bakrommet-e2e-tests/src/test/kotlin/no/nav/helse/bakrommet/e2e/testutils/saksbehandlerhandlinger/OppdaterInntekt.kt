package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.InntektRequestDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.helse.bakrommet.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

/**
 * Oppdater inntekt som returnerer ApiResult for å kunne teste både success og feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.oppdaterInntekt(
    personId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
    inntektRequest: InntektRequestDto,
    token: String = TestOppsett.userToken,
): ApiResult<Unit> =
    client
        .put("/v1/$personId/behandlinger/$behandlingId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(inntektRequest))
        }.let {
            it.result {
                assertEquals(HttpStatusCode.NoContent, it.status)
            }
        }

/**
 * @deprecated Bruk oppdaterInntekt som returnerer ApiResult i stedet
 */
@Deprecated("Bruk oppdaterInntekt som returnerer ApiResult", ReplaceWith("oppdaterInntekt(personId, behandlingId, yrkesaktivitetId, inntektRequest)"))
internal suspend fun ApplicationTestBuilder.oppdaterInntektOld(
    personId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
    inntektRequest: InntektRequestDto,
    expectedResponseStatus: HttpStatusCode = HttpStatusCode.NoContent,
) {
    val response =
        client.put("/v1/$personId/behandlinger/$behandlingId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(inntektRequest))
        }

    assertEquals(expectedResponseStatus, response.status, "Inntektsoppdatering skal returnere status ${expectedResponseStatus.value}")
}
