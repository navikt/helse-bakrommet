package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetCreateRequestDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.*

internal suspend fun ApplicationTestBuilder.opprettYrkesaktivitet(
    personId: UUID,
    behandlingId: UUID,
    kategorisering: YrkesaktivitetKategoriseringDto,
): UUID {
    val response =
        client.post("/v1/$personId/behandlinger/$behandlingId/yrkesaktivitet") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(YrkesaktivitetCreateRequestDto(kategorisering).serialisertTilString())
        }

    assertEquals(201, response.status.value, "Yrkesaktivitet skal opprettes med status 201")
    val body = response.body<JsonNode>()

    assertTrue(body.has("id"), "Response skal inneholde yrkesaktivitet ID")

    val yrkesaktivitetId = UUID.fromString(body["id"].asText())

    return yrkesaktivitetId
}
