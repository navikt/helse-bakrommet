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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.UUID

internal suspend fun ApplicationTestBuilder.opprettFrilanserYrkesaktivitet(
    periodeId: UUID,
    personId: String,
): UUID {
    val kategorisering =
        """
        {
            "kategorisering": {
                "INNTEKTSKATEGORI": "FRILANSER",
                "ORGNUMMER": "987654321",
                "ER_SYKMELDT": "ER_SYKMELDT_JA",
                "FRILANSER_FORSIKRING": "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG"
            }
        }
        """.trimIndent()

    val response =
        client.post("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(kategorisering)
        }

    assertEquals(201, response.status.value, "Frilanser yrkesaktivitet skal opprettes med status 201")
    val body = response.body<JsonNode>()

    assertTrue(body.has("id"), "Response skal inneholde yrkesaktivitet ID")
    assertTrue(body.has("kategorisering"), "Response skal inneholde kategorisering")

    val yrkesaktivitetId = UUID.fromString(body["id"].asText())
    assertTrue(yrkesaktivitetId != null, "Yrkesaktivitet ID skal være gyldig UUID")

    assertEquals("FRILANSER", body["kategorisering"]["INNTEKTSKATEGORI"].asText(), "Inntektskategori skal være FRILANSER")
    assertEquals("987654321", body["kategorisering"]["ORGNUMMER"].asText(), "Organisasjonsnummer skal være satt")
    assertEquals("ER_SYKMELDT_JA", body["kategorisering"]["ER_SYKMELDT"].asText(), "Er sykmeldt skal være JA")
    assertEquals("FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG", body["kategorisering"]["FRILANSER_FORSIKRING"].asText(), "Frilanser forsikring skal være 100% fra første sykedag")

    return yrkesaktivitetId
}
