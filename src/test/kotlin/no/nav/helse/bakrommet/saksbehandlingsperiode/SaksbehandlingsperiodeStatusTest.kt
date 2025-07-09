package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class SaksbehandlingsperiodeStatusTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
    }

    @Test
    fun `oppretter saksbehandlingsperiode`() =
        runApplicationTest {
            it.personDao.opprettPerson(fnr, personId)
            val createResponse =
                client.post("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31" }
                        """.trimIndent(),
                    )
                }
            assertEquals(201, createResponse.status.value)

            val periode = createResponse.body<Saksbehandlingsperiode>().truncateTidspunkt()

            assertEquals(
                SaksbehandlingsperiodeStatus.UNDER_BEHANDLING,
                periode.status,
                "Status skal være UNDER_BEHANDLING for nyopprettet saksbehandlingsperiode",
            )

            client.endreStatus(periode, nyStatus = "GODKJENT", forventetStatusCode = 400)
            client.endreStatus(
                periode,
                nyStatus = "TIL_BESLUTNING",
                forventetStatusCode = 200,
                forventResultat =
                    periode.copy(status = SaksbehandlingsperiodeStatus.TIL_BESLUTNING),
            )
            client.endreStatus(
                periode,
                nyStatus = "GODKJENT",
                forventetStatusCode = 200,
                forventResultat =
                    periode.copy(status = SaksbehandlingsperiodeStatus.GODKJENT),
            )
            client.endreStatus(
                periode,
                nyStatus = "TIL_BESLUTNING",
                forventetStatusCode = 400,
            )
        }

    private suspend fun HttpClient.endreStatus(
        periode: Saksbehandlingsperiode,
        nyStatus: String,
        forventetStatusCode: Int,
        forventResultat: Saksbehandlingsperiode? = null,
        token: String = TestOppsett.userToken,
    ): Saksbehandlingsperiode? {
        post("/v1/$personId/saksbehandlingsperioder/${periode.id}/status/$nyStatus") {
            bearerAuth(token)
        }.let { response ->
            assertEquals(forventetStatusCode, response.status.value)
            val returnertPeriode =
                try {
                    response.body<Saksbehandlingsperiode>()
                } catch (e: Exception) {
                    null
                }
            if (forventResultat != null) {
                assertNotNull(returnertPeriode)
                assertEquals(forventResultat.truncateTidspunkt(), returnertPeriode.truncateTidspunkt())

                get("/v1/$personId/saksbehandlingsperioder/${periode.id}") {
                    bearerAuth(token)
                }.let { getResponse ->
                    assertEquals(
                        forventResultat.truncateTidspunkt(),
                        getResponse.body<Saksbehandlingsperiode>()
                            .truncateTidspunkt(),
                        "GET på ny skal reflektere det som ble returnert fra POST",
                    )
                }
            }
            return returnertPeriode
        }
    }
}
