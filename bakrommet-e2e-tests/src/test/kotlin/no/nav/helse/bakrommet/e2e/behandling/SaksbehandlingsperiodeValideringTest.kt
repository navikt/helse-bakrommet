package no.nav.helse.bakrommet.e2e.behandling

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*

class SaksbehandlingsperiodeValideringTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
        val PERSON_PSEUDO_ID = UUID.nameUUIDFromBytes(PERSON_ID.toByteArray())
    }

    @Test
    fun `returnerer 400 hvis fom er etter tom`() {
        runApplicationTest { daoer ->
            daoer.personPseudoIdDao.opprettPseudoId(PERSON_PSEUDO_ID, NaturligIdent(FNR))
            val response =
                client.post("/v1/${PERSON_PSEUDO_ID}/behandlinger") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-31", "tom": "2023-01-01" }
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)

            val responsJson = assertDoesNotThrow { objectMapper.readTree(response.bodyAsText()) }
            assertEquals(400, responsJson["status"].asInt())
            assertEquals("Fom-dato kan ikke være etter tom-dato", responsJson["title"].asText())
            assertTrue(responsJson["type"].asText().endsWith("validation/input"))
        }
    }
}
