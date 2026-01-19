package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.vilkaar.OppdaterVilkaarsvurderingResponseDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.oppdaterVilkårsvurdering(
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
