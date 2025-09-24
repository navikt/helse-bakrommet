package no.nav.helse.bakrommet.inntektsmelding

import io.ktor.client.request.*
import io.ktor.client.statement.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InntektsmeldingRouteTest {
    @Test
    fun `Inntektsmelding-svar proxes pt rett gjennom`() =
        runApplicationTest(
            inntektsmeldingClient = InntektsmeldingApiMock.inntektsmeldingClientMock(),
        ) {
            val personId = "a0001"
            it.personDao.opprettPerson(InntektsmeldingApiMock.Person1.fnr, personId)

            client.get("/v1/$personId/inntektsmeldinger?fom=2020-01-01&tom=2025-06-01") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(200, status.value)
                assertEquals(InntektsmeldingApiMock.Person1.resp.asJsonNode(), bodyAsText().asJsonNode())
            }
        }

    @Test
    fun `404 fra Inntektsmelding gir tom liste i retur`() =
        runApplicationTest(
            aInntektClient = AInntektMock.aInntektClientMock(),
        ) {
            val personIdUnknown = "abcde"
            it.personDao.opprettPerson("9999999999", personIdUnknown)

            client.get("/v1/$personIdUnknown/inntektsmeldinger?fom=2020-01-01&tom=2025-06-01") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(200, status.value)
                assertEquals("[]".asJsonNode(), bodyAsText().asJsonNode())
            }
        }
}
