package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.PerioderDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.helse.bakrommet.serialisertTilString
import java.util.UUID
import kotlin.test.assertEquals

internal suspend fun ApplicationTestBuilder.oppdaterYrkesaktivitetsperioder(
    personPseudoId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
    perioder: PerioderDto,
): ApiResult<Unit> =
    client
        .put("/v1/$personPseudoId/behandlinger/$behandlingId/yrkesaktivitet/$yrkesaktivitetId/perioder") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(perioder.serialisertTilString())
        }.let {
            it.result {
                assertEquals(HttpStatusCode.NoContent, it.status)
            }
        }
