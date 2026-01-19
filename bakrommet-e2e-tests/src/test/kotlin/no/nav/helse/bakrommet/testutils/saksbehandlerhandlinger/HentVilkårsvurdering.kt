package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.util.objectMapper
import java.util.UUID

internal suspend fun ApplicationTestBuilder.hentVilkårsvurdering(
    personId: UUID,
    behandlingId: UUID,
): List<VilkaarsvurderingDto> {
    val response =
        client.get("/v1/$personId/behandlinger/$behandlingId/vilkaarsvurdering") {
            bearerAuth(TestOppsett.userToken)
        }

    val json = response.body<String>()
    return objectMapper.readValue<List<VilkaarsvurderingDto>>(json)
}
