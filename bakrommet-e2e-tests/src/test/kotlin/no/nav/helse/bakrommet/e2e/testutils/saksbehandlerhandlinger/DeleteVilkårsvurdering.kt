package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import java.util.UUID

suspend fun ApplicationTestBuilder.deleteVilkårsvurdering(
    personPseudoId: UUID,
    behandlingId: UUID,
    hovedspørsmål: String,
): ApiResult<Unit> =
    client
        .delete(
            "/v1/$personPseudoId/behandlinger/$behandlingId/vilkaarsvurdering/$hovedspørsmål",
        ) {
            bearerAuth(TestOppsett.userToken)
        }.result<Unit> { }
