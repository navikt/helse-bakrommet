package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.DagDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.DagerSomSkalOppdateresDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.settDagoversikt(
    personId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
    dager: List<DagDto>,
): ApiResult<Unit> {
    val req = DagerSomSkalOppdateresDto(dager)
    val response =
        client.put("/v1/$personId/behandlinger/$behandlingId/yrkesaktivitet/$yrkesaktivitetId/dagoversikt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(req.serialisertTilString())
        }
    return response.result {
        assertEquals(HttpStatusCode.NoContent, response.status)
    }
}
