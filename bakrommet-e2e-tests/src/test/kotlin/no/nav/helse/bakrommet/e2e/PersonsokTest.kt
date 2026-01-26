package no.nav.helse.bakrommet.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.errorhandling.ProblemDetails
import no.nav.helse.bakrommet.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersonsokTest {
    @Test
    fun `henter identer fra PDL`() =
        runApplicationTest {
            val response =
                client.post("/v1/personsok") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "ident": "01010199999" }
                        """.trimIndent(),
                    )
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(200, response.status.value)
            val regex = Regex("""\{"personId":"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"}""")
            assertTrue(response.bodyAsText().matches(regex))
        }

    @Test
    fun `får 400 problem details ved for kort input`() =
        runApplicationTest {
            val response =
                client.post("/v1/personsok") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "ident": "123" }
                        """.trimIndent(),
                    )
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(400, response.status.value)
            val problemdetails = response.tilProblemDetails()
            assertEquals(400, problemdetails.status)
            assertEquals("https://spillerom.ansatt.nav.no/validation/input", problemdetails.type)
            assertEquals("Naturlig ident må være 11 sifre", problemdetails.title)
        }

    @Test
    fun `får 404 problem details ved ikke funnet`() =
        runApplicationTest {
            val response =
                client.post("/v1/personsok") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "ident": "40440499999" }
                        """.trimIndent(),
                    )
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(404, response.status.value)
            val problemdetails = response.tilProblemDetails()
            assertEquals(404, problemdetails.status)
            assertEquals("about:blank", problemdetails.type)
            assertEquals("Person ikke funnet", problemdetails.title)
        }

    @Test
    fun `funker ikke med feil token`() =
        runApplicationTest {
            val response =
                client.post("/v1/personsok") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "ident": "01010199999" }
                        """.trimIndent(),
                    )
                    bearerAuth(TestOppsett.oAuthMock.token("FEIL-AUDIENCE"))
                }
            assertEquals(401, response.status.value)
        }

    private suspend fun HttpResponse.tilProblemDetails(): ProblemDetails =
        objectMapper.readValue(
            bodyAsText(),
            ProblemDetails::class.java,
        )
}
