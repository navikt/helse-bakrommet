package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.SykepengegrunnlagResponseDto
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.OpprettSykepengegrunnlagRequest
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.opprettSykepengegrunnlag(
    personId: UUID,
    behandlingId: UUID,
    req: OpprettSykepengegrunnlagRequest,
): SykepengegrunnlagResponseDto {
    val response =
        client.post("/v2/$personId/behandlinger/$behandlingId/sykepengegrunnlag") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(req.serialisertTilString())
        }
    assertEquals(201, response.status.value)
    return objectMapper.readValue(response.bodyAsText())
}
