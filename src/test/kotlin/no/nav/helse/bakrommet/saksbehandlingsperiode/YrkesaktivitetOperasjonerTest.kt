package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDTO
import no.nav.helse.bakrommet.testutils.print
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.deserialize
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*

class YrkesaktivitetOperasjonerTest {
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
            val opprettetYrkesaktivitetId =
                client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(kategorisering)
                }.let { response ->
                    assertEquals(201, response.status.value)
                    val body = response.body<JsonNode>()
                    assertEquals(kategorisering.asJsonNode()["kategorisering"], body["kategorisering"])
                    val id = body["id"].asText()
                    assertDoesNotThrow { UUID.fromString(id) }
                    assertDoesNotThrow { body.deserialize<YrkesaktivitetDTO>() }
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
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$opprettetYrkesaktivitetId/kategorisering") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(nyKategorisering)
            }.let { response ->
                assertEquals(204, response.status.value)
            }

            // Verifiser at kategorisering ble oppdatert
            daoer.yrkesaktivitetDao.hentYrkesaktivitetFor(periode).also { inntektsforholdFraDB ->
                inntektsforholdFraDB.filter { it.id == opprettetYrkesaktivitetId }.also {
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
                setBody("""{ "fom": "2023-01-01", "tom": "2023-01-10" }""")
            }

            val periode =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }.body<List<Saksbehandlingsperiode>>().first()

            // Opprett inntektsforhold med dagoversikt
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{"kategorisering": {"ER_SYKMELDT": "ER_SYKMELDT_JA"}}""")
            }

            val yrkesaktivitetId =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                    bearerAuth(TestOppsett.userToken)
                }.body<List<YrkesaktivitetDTO>>().first().id

            // Oppdater dagoversikt med array av spesifikke dager
            val oppdateringer = """[
                {
                    "dato": "2023-01-02",
                    "dagtype": "Syk",
                    "grad": 100,
                    "avvistBegrunnelse": []
                },
                {
                    "dato": "2023-01-03",
                    "dagtype": "Arbeidsdag", 
                    "grad": 0,
                    "avvistBegrunnelse": []
                },
                {
                    "dato": "2023-01-07",
                    "dagtype": "Syk",
                    "grad": 50,
                    "avvistBegrunnelse": []
                }
            ]"""

            val response =
                client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$yrkesaktivitetId/dagoversikt") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(oppdateringer)
                }
            val responseTekst = response.bodyAsText()
            print(responseTekst)
            assertEquals(HttpStatusCode.NoContent, response.status)

            // Verifiser at dagoversikten er oppdatert korrekt
            val oppdatertYrkesaktivitet = daoer.yrkesaktivitetDao.hentYrkesaktivitet(yrkesaktivitetId)!!
            val dagoversikt = oppdatertYrkesaktivitet.dagoversikt!!
            assertTrue(dagoversikt.isArray)

            val dager =
                dagoversikt.map { dag ->
                    Triple(
                        dag["dato"].asText(),
                        dag["dagtype"].asText(),
                        if (dag["kilde"].isNull) null else dag["kilde"].asText(),
                    )
                }

            // Verifiser at spesifiserte arbeidsdager er oppdatert med kilde Saksbehandler
            assertTrue(
                dager.any { (dato, dagtype, kilde) ->
                    dato == "2023-01-02" && dagtype == "Syk" && kilde == "Saksbehandler"
                },
            )
            assertTrue(
                dager.any { (dato, dagtype, kilde) ->
                    dato == "2023-01-03" && dagtype == "Arbeidsdag" && kilde == "Saksbehandler"
                },
            )

            // Verifiser at helgedager ikke er oppdatert (bevarer opprinnelig kilde null)
            val helgedager =
                dager.filter { (dato, dagtype, _) ->
                    dagtype == "Helg"
                }
            helgedager.forEach { (_, _, kilde) ->
                assertEquals(null, kilde, "Helgedager skal fortsatt ha kilde null")
            }

            // Verifiser at dag 2023-01-07 (lørdag/helg) ikke ble oppdatert selv om den var i oppdateringslisten
            val dag7 = dager.find { (dato, _, _) -> dato == "2023-01-07" }
            if (dag7 != null) {
                assertEquals("Helg", dag7.second, "2023-01-07 skal fortsatt være helg")
                assertEquals(null, dag7.third, "2023-01-07 skal fortsatt ha kilde null")
            }
        }
    }

    @Test
    fun `oppdaterer dagoversikt for inntektsforhold med nytt format`() {
        runApplicationTest { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            client.post("/v1/$PERSON_ID/saksbehandlingsperioder") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{ "fom": "2023-01-01", "tom": "2023-01-10" }""")
            }

            val periode =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }.body<List<Saksbehandlingsperiode>>().first()

            // Opprett inntektsforhold med dagoversikt
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{"kategorisering": {"ER_SYKMELDT": "ER_SYKMELDT_JA"}}""")
            }

            val yrkesaktivitetId =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                    bearerAuth(TestOppsett.userToken)
                }.body<List<YrkesaktivitetDTO>>().first().id

            // Oppdater dagoversikt med nytt format (objekt med dager og notat)
            val oppdateringer = """{
                "dager": [
                    {
                        "dato": "2023-01-02",
                        "dagtype": "Syk",
                        "grad": 100,
                        "avvistBegrunnelse": []
                    },
                    {
                        "dato": "2023-01-03",
                        "dagtype": "Arbeidsdag", 
                        "grad": 0,
                        "avvistBegrunnelse": []
                    }
                ],
                "notat": "Test notat"
            }"""

            val response =
                client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$yrkesaktivitetId/dagoversikt") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(oppdateringer)
                }
            val responseTekst = response.bodyAsText()
            print(responseTekst)
            assertEquals(HttpStatusCode.NoContent, response.status)

            // Verifiser at dagoversikten er oppdatert korrekt
            val oppdatertYrkesaktivitet = daoer.yrkesaktivitetDao.hentYrkesaktivitet(yrkesaktivitetId)!!
            val dagoversikt = oppdatertYrkesaktivitet.dagoversikt!!
            assertTrue(dagoversikt.isArray)

            val dager =
                dagoversikt.map { dag ->
                    Triple(
                        dag["dato"].asText(),
                        dag["dagtype"].asText(),
                        if (dag["kilde"].isNull) null else dag["kilde"].asText(),
                    )
                }

            // Verifiser at spesifiserte arbeidsdager er oppdatert med kilde Saksbehandler
            assertTrue(
                dager.any { (dato, dagtype, kilde) ->
                    dato == "2023-01-02" && dagtype == "Syk" && kilde == "Saksbehandler"
                },
            )
            assertTrue(
                dager.any { (dato, dagtype, kilde) ->
                    dato == "2023-01-03" && dagtype == "Arbeidsdag" && kilde == "Saksbehandler"
                },
            )

            // Verifiser at helgedager ikke er oppdatert (bevarer opprinnelig kilde null)
            val helgedager =
                dager.filter { (dato, dagtype, _) ->
                    dagtype == "Helg"
                }
            helgedager.forEach { (_, _, kilde) ->
                assertEquals(null, kilde, "Helgedager skal fortsatt ha kilde null")
            }
        }
    }
}
