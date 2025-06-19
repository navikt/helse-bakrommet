package no.nav.helse.bakrommet.aareg

import io.ktor.client.request.*
import io.ktor.client.statement.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArbeidsforholdRouteTest {
    @Test
    fun `403 fra AA-reg gir 403 videre med feilbeskrivelse`() =
        runApplicationTest(
            aaRegClient = AARegMock.aaRegClientMock(),
        ) {
            val personIdForbidden = "ab403"
            it.personDao.opprettPerson("01019000" + "403", personIdForbidden)

            client.get("/v1/$personIdForbidden/arbeidsforhold") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(403, status.value)
                assertEquals(
                    """
                    {"type":"about:blank","title":"Ingen tilgang","status":403,"detail":"Ikke tilstrekkelig tilgang i AA-REG","instance":null}
                """.asJsonNode(),
                    bodyAsText().asJsonNode(),
                )
            }
        }

    @Test
    fun `AA-reg-svar proxes pt rett gjennom`() =
        runApplicationTest(
            aaRegClient = AARegMock.aaRegClientMock(),
        ) {
            val personId = "a0001"
            it.personDao.opprettPerson(AARegMock.Person1.fnr, personId)

            client.get("/v1/$personId/arbeidsforhold") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(200, status.value)
                assertEquals(AARegMock.Person1.respV2.asJsonNode(), bodyAsText().asJsonNode())
            }
        }
}
