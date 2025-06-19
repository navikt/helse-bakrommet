package no.nav.helse.bakrommet.ainntekt

import io.ktor.client.request.*
import io.ktor.client.statement.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AInntektRouteTest {
    @Test
    fun `AInntekt-svar proxes pt rett gjennom`() =
        runApplicationTest(
            aInntektClient = AInntektMock.aInntektClientMock(),
        ) {
            val personId = "a0001"
            it.personDao.opprettPerson(AInntektMock.Person1.fnr, personId)

            client.get("/v1/$personId/ainntekt?fom=2024-01&tom=2025-02") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(200, status.value)
                assertEquals(AInntektMock.Person1.resp.asJsonNode(), bodyAsText().asJsonNode())
            }
        }

    @Test
    fun `403 fra Inntektskomponenten gir 403 videre med feilbeskrivelse`() =
        runApplicationTest(
            aInntektClient = AInntektMock.aInntektClientMock(),
        ) {
            val personIdForbidden = "ab403"
            it.personDao.opprettPerson("01019000" + "403", personIdForbidden)

            client.get("/v1/$personIdForbidden/ainntekt?fom=2024-01&tom=2025-02") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(403, status.value)
                assertEquals(
                    """
                    {"type":"about:blank","title":"Ingen tilgang","status":403,"detail":"Ikke tilstrekkelig tilgang i A-Inntekt","instance":null}
                """.asJsonNode(),
                    bodyAsText().asJsonNode(),
                )
            }
        }
}
