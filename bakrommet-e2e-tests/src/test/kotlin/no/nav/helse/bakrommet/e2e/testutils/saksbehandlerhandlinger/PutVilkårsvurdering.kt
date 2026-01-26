package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.vilkaar.OppdaterVilkaarsvurderingResponseDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingRequestDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.helse.bakrommet.serialisertTilString
import java.util.UUID

suspend fun ApplicationTestBuilder.putVilkårsvurdering(
    personPseudoId: UUID,
    behandlingId: UUID,
    hovedspørsmål: String,
    vurdering: VurderingDto,
    notat: String?,
    vilkårskode: String = "ET_VILKÅR",
): ApiResult<OppdaterVilkaarsvurderingResponseDto> {
    val req =
        VilkaarsvurderingRequestDto(
            vurdering = vurdering,
            vilkårskode = vilkårskode,
            underspørsmål = emptyList(),
            notat = notat,
        )
    return client
        .put(
            "/v1/$personPseudoId/behandlinger/$behandlingId/vilkaarsvurdering/$hovedspørsmål",
        ) {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(req.serialisertTilString())
        }.let {
            it.result<OppdaterVilkaarsvurderingResponseDto> {}
        }
}
