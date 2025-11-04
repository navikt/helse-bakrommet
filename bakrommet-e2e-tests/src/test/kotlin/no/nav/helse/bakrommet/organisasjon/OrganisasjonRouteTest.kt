package no.nav.helse.bakrommet.organisasjon

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OrganisasjonRouteTest {
    @Test
    fun `hent organisasjonsnavn for mocket organisasjon`() =
        runApplicationTest {
            val orgnummer = "987654321"
            val forventetNavn = "Kranførerkompaniet"

            client
                .get("/v1/organisasjon/$orgnummer") {
                    header(HttpHeaders.Authorization, "Bearer ${TestOppsett.userToken}")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(forventetNavn, bodyAsText())
                }
        }

    @Test
    fun `hent organisasjonsnavn for annen mocket organisasjon`() =
        runApplicationTest {
            val orgnummer = "123456789"
            val forventetNavn = "Krankompisen"

            client
                .get("/v1/organisasjon/$orgnummer") {
                    header(HttpHeaders.Authorization, "Bearer ${TestOppsett.userToken}")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(forventetNavn, bodyAsText())
                }
        }

    @Test
    fun `hent organisasjonsnavn for faker kotlin generert organisasjon`() =
        runApplicationTest {
            val orgnummer = "876547463"
            val forventetNavn = "Eide BA"

            client
                .get("/v1/organisasjon/$orgnummer") {
                    header(HttpHeaders.Authorization, "Bearer ${TestOppsett.userToken}")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(forventetNavn, bodyAsText())
                }
        }

    @Test
    fun `organisasjon ikke funnet gir 404`() =
        runApplicationTest {
            val orgnummer = "199999999" // Ikke i mock data

            client
                .get("/v1/organisasjon/$orgnummer") {
                    header(HttpHeaders.Authorization, "Bearer ${TestOppsett.userToken}")
                }.apply {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
        }
}
