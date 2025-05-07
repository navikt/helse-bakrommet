package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.*
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SaksbehandlingsperiodeTest {
    companion object {
        val fnr = "01019012349"
        val personId = "65hth"

        @JvmStatic
        @BeforeAll
        fun setOpp() {
            val dao = PersonDao(TestDataSource.dataSource)
            dao.opprettPerson(fnr, personId)
        }
    }

    @Test
    fun `oppretter saksbehandlingsperiode`() =
        runApplicationTest {
            val response =
                client.post("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31" }
                        """.trimIndent(),
                    )
                }
            assertEquals(201, response.status.value)

            val saksbehandlingsperiode: Saksbehandlingsperiode =
                objectMapper.readValue(
                    response.bodyAsText(),
                    Saksbehandlingsperiode::class.java,
                )
            saksbehandlingsperiode.fom.toString() `should equal` "2023-01-01"
            saksbehandlingsperiode.tom.toString() `should equal` "2023-01-31"
            saksbehandlingsperiode.spilleromPersonId `should equal` personId
            saksbehandlingsperiode.opprettetAvNavIdent `should equal` "tullebruker"
            saksbehandlingsperiode.opprettetAvNavn `should equal` "Tulla Bruker"

            val allePerioder =
                client.get("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(200, allePerioder.status.value)
            val perioder: List<Saksbehandlingsperiode> =
                objectMapper.readValue(
                    allePerioder.bodyAsText(),
                    objectMapper.typeFactory.constructCollectionType(
                        List::class.java,
                        Saksbehandlingsperiode::class.java,
                    ),
                )
            perioder.size `should equal` 1
            perioder `should equal` listOf(saksbehandlingsperiode)
        }
}
