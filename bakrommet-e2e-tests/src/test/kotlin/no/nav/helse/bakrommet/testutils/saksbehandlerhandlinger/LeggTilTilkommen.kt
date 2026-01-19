package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.tilkommen.OpprettTilkommenInntektRequestDto
import no.nav.helse.bakrommet.api.dto.tilkommen.TilkommenInntektResponseDto
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.leggTilTilkommenInntekt(
    personId: UUID,
    behandlingId: UUID,
    tilkommenInntekt: OpprettTilkommenInntektRequestDto,
): TilkommenInntektResponseDto {
    leggTilTilkommenInntekt(
        personId = personId,
        behandlingId = behandlingId,
        tilkommenInntekt = tilkommenInntekt,
        forventetResponseKode = HttpStatusCode.Created,
    ).apply {
        return objectMapper.readValue(this)
    }
}

internal suspend fun ApplicationTestBuilder.leggTilTilkommenInntekt(
    personId: UUID,
    behandlingId: UUID,
    tilkommenInntekt: OpprettTilkommenInntektRequestDto,
    forventetResponseKode: HttpStatusCode,
): String {
    val response =
        client.post("/v1/$personId/behandlinger/$behandlingId/tilkommeninntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(tilkommenInntekt.serialisertTilString())
        }
    assertEquals(forventetResponseKode, response.status)
    return response.bodyAsText()
}
