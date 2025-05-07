package no.nav.helse.bakrommet.sykepengesoknad

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.*
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDao
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SoknaderTest {
    val mockSoknaderClient =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer ${TestOppsett.oboToken}") {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                println(request.url)
                val reply = "[]"
                respond(
                    status = HttpStatusCode.OK,
                    content = reply,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }

    companion object {
        val fnr = "01019012345"
        val personId = "abcde"

        @JvmStatic
        @BeforeAll
        fun setOpp() {
            val dao = PersonDao(TestDataSource.dataSource)
            dao.opprettPerson(fnr, personId)
        }
    }

    @Test
    fun `ingen søknader`() =
        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadBackendClient(
                    configuration = Configuration.SykepengesoknadBackend("soknadHost", "soknadScope"),
                    httpClient = mockSoknaderClient,
                ),
        ) {
            val response =
                client.get("/v1/$personId/soknader") {
                    bearerAuth(TestOppsett.userToken)
                }
            Assertions.assertEquals(200, response.status.value)
            Assertions.assertEquals("[]", response.bodyAsText())
        }
}
