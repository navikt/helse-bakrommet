package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDTO
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.deserialize
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*

class InntektsforholdOperasjonerTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `oppretter og oppdaterer inntektsforhold`() {
        runApplicationTest { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett saksbehandlingsperiode
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{ "fom": "2023-01-01", "tom": "2023-01-31" }""")
            }

            val periode =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }.body<List<Saksbehandlingsperiode>>().first()

            @Language("json")
            val kategorisering =
                """
                {
                    "kategorisering": {
                        "INNTEKTSKATEGORI": "SELVSTENDIG_NÆRINGSDRIVENDE",
                        "TYPE_SELVSTENDIG_NÆRINGSDRIVENDE": "FISKER",
                        "FISKER_BLAD": "FISKER_BLAD_B",
                        "ER_SYKMELDT": "ER_SYKMELDT_JA"
                    }
                }
                """.trimIndent()

            // Opprett inntektsforhold
            val opprettetInntektsforholdId =
                client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/inntektsforhold") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(kategorisering)
                }.let { response ->
                    assertEquals(201, response.status.value)
                    val body = response.body<JsonNode>()
                    assertEquals(kategorisering.asJsonNode()["kategorisering"], body["kategorisering"])
                    val id = body["id"].asText()
                    assertDoesNotThrow { UUID.fromString(id) }
                    assertDoesNotThrow { body.deserialize<InntektsforholdDTO>() }
                    UUID.fromString(id)
                }

            @Language("json")
            val nyKategorisering =
                """
                {
                    "INNTEKTSKATEGORI": "ARBEIDSTAKER_ELLER_NOE_SÅNT",
                    "ER_SYKMELDT": "ER_SYKMELDT_NEI"
                }
                """.trimIndent()

            // Oppdater kategorisering
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/inntektsforhold/$opprettetInntektsforholdId/kategorisering") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(nyKategorisering)
            }.let { response ->
                assertEquals(204, response.status.value)
            }

            // Verifiser at kategorisering ble oppdatert
            daoer.inntektsforholdDao.hentInntektsforholdFor(periode).also { inntektsforholdFraDB ->
                inntektsforholdFraDB.filter { it.id == opprettetInntektsforholdId }.also {
                    assertEquals(1, it.size)
                    assertEquals(nyKategorisering.asJsonNode(), it.first().kategorisering)
                }
            }
        }
    }

    @Test
    fun `oppdaterer dagoversikt for inntektsforhold`() {
        runApplicationTest { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            client.post("/v1/$PERSON_ID/saksbehandlingsperioder") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{ "fom": "2023-01-01", "tom": "2023-01-31" }""")
            }

            val periode =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }.body<List<Saksbehandlingsperiode>>().first()

            // Opprett inntektsforhold
            val inntektsforholdId =
                client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/inntektsforhold") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody("""{"kategorisering": {"INNTEKTSKATEGORI": "ARBEIDSTAKER"}}""")
                }.let { response ->
                    UUID.fromString(response.body<JsonNode>()["id"].asText())
                }

            @Language("json")
            val dagoversikt =
                """
                [
                    {
                        "id": "dag-1",
                        "type": "SYKEDAG",
                        "dato": "2023-01-01"
                    },
                    {
                        "id": "dag-2",
                        "type": "HELGEDAG",
                        "dato": "2023-01-02"
                    },
                    {
                        "id": "dag-3",
                        "type": "SYKEDAG",
                        "dato": "2023-01-03"
                    }
                ]
                """.trimIndent()

            // Oppdater dagoversikt
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/inntektsforhold/$inntektsforholdId/dagoversikt") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(dagoversikt)
            }.let { response ->
                assertEquals(204, response.status.value)
            }

            // Verifiser at dagoversikt ble oppdatert
            daoer.inntektsforholdDao.hentInntektsforholdFor(periode).also { inntektsforholdFraDB ->
                inntektsforholdFraDB.filter { it.id == inntektsforholdId }.also {
                    assertEquals(1, it.size)
                    assertEquals(dagoversikt.asJsonNode(), it.first().dagoversikt)
                }
            }
        }
    }
}
