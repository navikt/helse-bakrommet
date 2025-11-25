package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetInntektRequest
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal
import java.util.UUID

internal suspend fun ApplicationTestBuilder.settInntekt(
    personId: String,
    periodeId: UUID,
    yrkesaktivitetId: UUID,
    inntekt: BigDecimal?,
) {
    val response =
        client.put("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/fri-inntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(objectMapperCustomSerde.writeValueAsString(YrkesaktivitetInntektRequest(inntekt)))
        }
    assertEquals(204, response.status.value)
}
