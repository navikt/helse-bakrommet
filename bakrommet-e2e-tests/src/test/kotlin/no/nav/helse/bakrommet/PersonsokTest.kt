package no.nav.helse.bakrommet

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
            val regex = Regex("""\{"personId":"[a-z0-9]{5}"}""")
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
            assertEquals("Ident må være 11 eller 13 siffer lang", problemdetails.title)
        }

    @Test
    fun `får 404 problem details ved ikke funnet`() =
        runApplicationTest {
            val response =
                client.post("/v1/personsok") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "ident": "99999999999" }
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
}
