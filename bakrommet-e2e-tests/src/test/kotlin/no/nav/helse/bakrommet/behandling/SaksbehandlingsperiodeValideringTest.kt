package no.nav.helse.bakrommet.behandling

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class SaksbehandlingsperiodeValideringTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `returnerer 400 hvis fom er etter tom`() {
        runApplicationTest { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)
            val response =
                client.post("/v1/$PERSON_ID/behandlinger") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-31", "tom": "2023-01-01" }
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)

            val responsJson = assertDoesNotThrow { runBlocking { objectMapper.readTree(response.bodyAsText()) } }
            assertEquals(400, responsJson["status"].asInt())
            assertEquals("Fom-dato kan ikke være etter tom-dato", responsJson["title"].asText())
            assertTrue(responsJson["type"].asText().endsWith("validation/input"))
        }
    }
}
