package no.nav.helse.bakrommet

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
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
            assertEquals("""[12345678910, 10987654321]""", response.headers["identer"])
            val regex = Regex("""\{ "personId": "[a-z0-9]{5}" }""")
            assertTrue(response.bodyAsText().matches(regex))
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
