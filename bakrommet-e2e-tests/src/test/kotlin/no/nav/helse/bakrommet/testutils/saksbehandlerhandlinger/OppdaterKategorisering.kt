package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.util.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.oppdaterKategorisering(
    personId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
    kategorisering: YrkesaktivitetKategoriseringDto,
    expectedStatus: HttpStatusCode = HttpStatusCode.NoContent,
) {
    val response =
        client.put("/v1/$personId/behandlinger/$behandlingId/yrkesaktivitet/$yrkesaktivitetId/kategorisering") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(kategorisering.serialisertTilString())
        }
    assertEquals(expectedStatus, response.status)
}
