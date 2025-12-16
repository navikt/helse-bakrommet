package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.SykepengegrunnlagResponseDto
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.OpprettSykepengegrunnlagRequest
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.opprettSykepengegrunnlag(
    personId: UUID,
    periodeId: UUID,
    req: OpprettSykepengegrunnlagRequest,
): SykepengegrunnlagResponseDto {
    val response =
        client.post("/v2/$personId/behandlinger/$periodeId/sykepengegrunnlag") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(req.serialisertTilString())
        }
    assertEquals(201, response.status.value)
    return objectMapper.readValue(response.bodyAsText())
}
