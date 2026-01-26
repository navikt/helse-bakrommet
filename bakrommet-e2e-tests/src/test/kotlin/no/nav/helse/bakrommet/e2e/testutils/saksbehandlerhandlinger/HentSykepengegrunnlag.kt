package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.SykepengegrunnlagResponseDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.objectMapper
import java.util.UUID

internal suspend fun ApplicationTestBuilder.hentSykepengegrunnlag(
    personId: UUID,
    behandlingId: UUID,
): SykepengegrunnlagResponseDto? {
    val response =
        client.get("/v2/$personId/behandlinger/$behandlingId/sykepengegrunnlag") {
            bearerAuth(TestOppsett.userToken)
        }

    val sykepengegrunnlagStr = response.body<String>()
    if (sykepengegrunnlagStr == "null") return null
    return objectMapper.readValue(sykepengegrunnlagStr, SykepengegrunnlagResponseDto::class.java)
}
