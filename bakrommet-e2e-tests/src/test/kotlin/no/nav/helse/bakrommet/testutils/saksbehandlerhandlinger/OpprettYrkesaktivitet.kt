package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetCreateRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.util.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.UUID

internal suspend fun ApplicationTestBuilder.opprettYrkesaktivitet(
    periodeId: UUID,
    personId: String,
    kategorisering: YrkesaktivitetKategorisering,
): UUID {
    val response =
        client.post("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(YrkesaktivitetCreateRequest(kategorisering).serialisertTilString())
        }

    assertEquals(201, response.status.value, "Yrkesaktivitet skal opprettes med status 201")
    val body = response.body<JsonNode>()

    assertTrue(body.has("id"), "Response skal inneholde yrkesaktivitet ID")

    val yrkesaktivitetId = UUID.fromString(body["id"].asText())

    return yrkesaktivitetId
}
