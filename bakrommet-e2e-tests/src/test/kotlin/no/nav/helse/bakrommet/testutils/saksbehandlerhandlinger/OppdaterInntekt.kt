package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.oppdaterInntekt(
    personId: String,
    periodeId: UUID,
    yrkesaktivitetId: UUID,
    inntektRequest: InntektRequest,
    expectedResponseStatus: HttpStatusCode = HttpStatusCode.NoContent,
) {
    val response =
        client.put("/v1/$personId/behandlinger/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(objectMapperCustomSerde.writeValueAsString(inntektRequest))
        }

    assertEquals(expectedResponseStatus, response.status, "Inntektsoppdatering skal returnere status ${expectedResponseStatus.value}")
}
