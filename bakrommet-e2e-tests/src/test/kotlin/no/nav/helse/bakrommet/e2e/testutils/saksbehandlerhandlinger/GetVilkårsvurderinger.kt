package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import org.junit.jupiter.api.Assertions
import java.util.UUID

suspend fun ApplicationTestBuilder.getVilkårsvurderinger(
    personPseudoId: UUID,
    behandlingId: UUID,
): ApiResult<List<VilkaarsvurderingDto>> =
    client
        .get("/v1/$personPseudoId/behandlinger/$behandlingId/vilkaarsvurdering") {
            bearerAuth(TestOppsett.userToken)
        }.let {
            it.result<List<VilkaarsvurderingDto>> {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }
        }
