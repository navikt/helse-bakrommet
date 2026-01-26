package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import java.util.UUID
import kotlin.test.assertEquals

internal suspend fun ApplicationTestBuilder.hentInntektsmeldinger(
    personPseudoId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
): ApiResult<List<Inntektsmelding>> =
    client
        .get("/v1/$personPseudoId/behandlinger/$behandlingId/yrkesaktivitet/$yrkesaktivitetId/inntektsmeldinger") {
            bearerAuth(TestOppsett.userToken)
        }.let {
            it.result {
                assertEquals(HttpStatusCode.OK, it.status)
            }
        }
