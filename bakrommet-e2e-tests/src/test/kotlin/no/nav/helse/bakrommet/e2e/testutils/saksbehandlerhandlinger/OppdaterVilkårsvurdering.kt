package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.vilkaar.OppdaterVilkaarsvurderingResponseDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

/**
 * Oppdater vilkårsvurdering som returnerer ApiResult for å kunne teste både success og feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.oppdaterVilkårsvurdering(
    personId: UUID,
    behandlingId: UUID,
    vilkår: VilkaarsvurderingDto,
    token: String = TestOppsett.userToken,
): ApiResult<OppdaterVilkaarsvurderingResponseDto> =
    client
        .put("/v1/$personId/behandlinger/$behandlingId/vilkaarsvurdering/${vilkår.hovedspørsmål}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(vilkår.serialisertTilString())
        }.let {
            it.result {
                assertEquals(HttpStatusCode.Created, it.status)
            }
        }

/**
 * @deprecated Bruk oppdaterVilkårsvurdering som returnerer ApiResult i stedet
 */
@Deprecated("Bruk oppdaterVilkårsvurdering som returnerer ApiResult", ReplaceWith("oppdaterVilkårsvurdering(personId, behandlingId, vilkår)"))
internal suspend fun ApplicationTestBuilder.oppdaterVilkårsvurderingOld(
    personId: UUID,
    behandlingId: UUID,
    vilkår: VilkaarsvurderingDto,
    expectedResponseStatus: HttpStatusCode = HttpStatusCode.Created,
): OppdaterVilkaarsvurderingResponseDto {
    val response =
        client.put("/v1/$personId/behandlinger/$behandlingId/vilkaarsvurdering/${vilkår.hovedspørsmål}") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(vilkår.serialisertTilString())
        }

    assertEquals(expectedResponseStatus, response.status, "Vilkårsvurdering lagring skal returnere status ${expectedResponseStatus.value}")

    return objectMapper.readValue(response.bodyAsText(), OppdaterVilkaarsvurderingResponseDto::class.java)
}
