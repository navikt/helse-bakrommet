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

internal suspend fun ApplicationTestBuilder.opprettArbeidstakerYrkesaktivitet(
    periodeId: UUID,
    personId: String,
    orgnr: String,
): UUID {
    val kategorisering =
        """
        {
            "kategorisering": {
                "INNTEKTSKATEGORI": "ARBEIDSTAKER",
                "ORGNUMMER": "$orgnr",
                "ER_SYKMELDT": "ER_SYKMELDT_JA",
                "TYPE_ARBEIDSTAKER": "ORDINÆRT_ARBEIDSFORHOLD"
            }
        }
        """.trimIndent()

    val response =
        client.post("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(kategorisering)
        }

    assertEquals(201, response.status.value, "Arbeidstaker yrkesaktivitet skal opprettes med status 201")
    val body = response.body<JsonNode>()

    assertTrue(body.has("id"), "Response skal inneholde yrkesaktivitet ID")
    assertTrue(body.has("kategorisering"), "Response skal inneholde kategorisering")

    val yrkesaktivitetId = UUID.fromString(body["id"].asText())
    assertTrue(yrkesaktivitetId != null, "Yrkesaktivitet ID skal være gyldig UUID")

    assertEquals("ARBEIDSTAKER", body["kategorisering"]["INNTEKTSKATEGORI"].asText(), "Inntektskategori skal være ARBEIDSTAKER")
    assertEquals(orgnr, body["kategorisering"]["ORGNUMMER"].asText(), "Organisasjonsnummer skal være satt")
    assertEquals("ER_SYKMELDT_JA", body["kategorisering"]["ER_SYKMELDT"].asText(), "Er sykmeldt skal være JA")
    assertEquals("ORDINÆRT_ARBEIDSFORHOLD", body["kategorisering"]["TYPE_ARBEIDSTAKER"].asText(), "Type arbeidstaker skal være ORDINÆRT")

    return yrkesaktivitetId
}
