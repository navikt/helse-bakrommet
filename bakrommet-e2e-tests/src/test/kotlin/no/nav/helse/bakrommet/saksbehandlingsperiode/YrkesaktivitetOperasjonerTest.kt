package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDTO
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.hentDekningsgrad
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
                client
                    .get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<Saksbehandlingsperiode>>()
                    .first()

            @Language("json")
            val kategorisering =
                """
                {
                    "kategorisering": {
                        "INNTEKTSKATEGORI": "SELVSTENDIG_NÆRINGSDRIVENDE",
                        "ER_SYKMELDT": "ER_SYKMELDT_JA",
                        "TYPE_SELVSTENDIG_NÆRINGSDRIVENDE": "FISKER"
                    }
                }
                """.trimIndent()

            // Opprett inntektsforhold
            val opprettetYrkesaktivitetId =
                client
                    .post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                        bearerAuth(TestOppsett.userToken)
                        contentType(ContentType.Application.Json)
                        setBody(kategorisering)
                    }.let { response ->
                        assertEquals(201, response.status.value)
                        val body = response.body<JsonNode>()
                        // Verifiser at viktige felter er med
                        assertEquals("SELVSTENDIG_NÆRINGSDRIVENDE", body["kategorisering"]["INNTEKTSKATEGORI"].asText())
                        assertEquals("FISKER", body["kategorisering"]["TYPE_SELVSTENDIG_NÆRINGSDRIVENDE"].asText())
                        val id = body["id"].asText()
                        assertDoesNotThrow { UUID.fromString(id) }
                        assertDoesNotThrow { body.deserialize<YrkesaktivitetDTO>() }
                        UUID.fromString(id)
                    }

            val nyKategorisering =
                HashMap<String, String>().apply {
                    put("INNTEKTSKATEGORI", "ARBEIDSTAKER")
                    put("ORGNUMMER", "123456789")
                    put("ER_SYKMELDT", "ER_SYKMELDT_NEI")
                    put("TYPE_ARBEIDSTAKER", "ORDINÆRT_ARBEIDSFORHOLD")
                }

            // Oppdater kategorisering
            client
                .put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$opprettetYrkesaktivitetId/kategorisering") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(nyKategorisering)
                }.let { response ->
                    assertEquals(204, response.status.value)
                }

            // Verifiser at kategorisering ble oppdatert
            daoer.yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(periode).also { inntektsforholdFraDB ->
                inntektsforholdFraDB.filter { it.id == opprettetYrkesaktivitetId }.also {
                    assertEquals(1, it.size)
                    assertEquals(nyKategorisering, it.first().kategorisering)
                }
            }

            // Verifiser at dekningsgrad er riktig etter oppdatering
            val oppdatertYrkesaktivitet = daoer.yrkesaktivitetDao.hentYrkesaktivitetDbRecord(opprettetYrkesaktivitetId)!!
            assertEquals(1.0, oppdatertYrkesaktivitet.hentDekningsgrad().verdi.prosentDesimal)
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
                client
                    .get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<Saksbehandlingsperiode>>()
                    .first()

            // Opprett inntektsforhold med dagoversikt
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """{"kategorisering": {"INNTEKTSKATEGORI": "ARBEIDSTAKER", "ORGNUMMER": "123456789", "ER_SYKMELDT": "ER_SYKMELDT_JA", "TYPE_ARBEIDSTAKER": "ORDINÆRT_ARBEIDSFORHOLD"}}""",
                )
            }

            val yrkesaktivitetId =
                client
                    .get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<YrkesaktivitetDTO>>()
                    .first()
                    .id

            // Oppdater dagoversikt med array av spesifikke dager
            val oppdateringer = """[
                {
                    "dato": "2023-01-02",
                    "dagtype": "Syk",
                    "grad": 100,
                    "avslåttBegrunnelse": []
                },
                {
                    "dato": "2023-01-03",
                    "dagtype": "Arbeidsdag", 
                    "grad": 0
                },
                {
                    "dato": "2023-01-07",
                    "dagtype": "Syk",
                    "grad": 50,
                    "avslåttBegrunnelse": []
                }
            ]"""

            val response =
                client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$yrkesaktivitetId/dagoversikt") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(oppdateringer)
                }
            assertEquals(HttpStatusCode.NoContent, response.status)

            client
                .get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                    bearerAuth(TestOppsett.userToken)
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                    val body = it.bodyAsText()
                    print(body)
                }
            // Verifiser at dagoversikten er oppdatert korrekt
            val oppdatertYrkesaktivitet = daoer.yrkesaktivitetDao.hentYrkesaktivitetDbRecord(yrkesaktivitetId)!!
            val dagoversikt = oppdatertYrkesaktivitet.dagoversikt!!
            assertTrue(dagoversikt.isNotEmpty())

            val dager =
                dagoversikt.map { dag ->
                    Triple(
                        dag.dato.toString(),
                        dag.dagtype.name,
                        dag.kilde?.name,
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

            // Verifiser at dekningsgrad er riktig
            assertEquals(1.0, oppdatertYrkesaktivitet.hentDekningsgrad().verdi.prosentDesimal)
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
                client
                    .get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<Saksbehandlingsperiode>>()
                    .first()

            // Opprett inntektsforhold med dagoversikt
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """{"kategorisering": {"INNTEKTSKATEGORI": "ARBEIDSTAKER", "ORGNUMMER": "123456789", "ER_SYKMELDT": "ER_SYKMELDT_JA", "TYPE_ARBEIDSTAKER": "ORDINÆRT_ARBEIDSFORHOLD"}}""",
                )
            }

            val yrkesaktivitetId =
                client
                    .get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<YrkesaktivitetDTO>>()
                    .first()
                    .id

            // Oppdater dagoversikt med nytt format (objekt med dager og notat)
            val oppdateringer = """{
                "dager": [
                    {
                        "dato": "2023-01-02",
                        "dagtype": "Syk",
                        "grad": 100,
                        "avslåttBegrunnelse": []
                    },
                    {
                        "dato": "2023-01-03",
                        "dagtype": "Arbeidsdag", 
                        "grad": 0,
                        "avslåttBegrunnelse": []
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
            val oppdatertYrkesaktivitet = daoer.yrkesaktivitetDao.hentYrkesaktivitetDbRecord(yrkesaktivitetId)!!
            val dagoversikt = oppdatertYrkesaktivitet.dagoversikt!!
            assertTrue(dagoversikt.isNotEmpty())

            val dager =
                dagoversikt.map { dag ->
                    Triple(
                        dag.dato.toString(),
                        dag.dagtype.name,
                        dag.kilde?.name,
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

            // Verifiser at dekningsgrad er riktig
            assertEquals(1.0, oppdatertYrkesaktivitet.hentDekningsgrad().verdi.prosentDesimal)
        }
    }

    @Test
    fun `oppdaterer perioder for inntektsforhold`() {
        runApplicationTest { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett saksbehandlingsperiode
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{ "fom": "2023-01-01", "tom": "2023-01-31" }""")
            }

            val periode =
                client
                    .get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<Saksbehandlingsperiode>>()
                    .first()

            // Opprett inntektsforhold
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """{"kategorisering": {"INNTEKTSKATEGORI": "ARBEIDSTAKER", "ORGNUMMER": "123456789", "ER_SYKMELDT": "ER_SYKMELDT_JA", "TYPE_ARBEIDSTAKER": "ORDINÆRT_ARBEIDSFORHOLD"}}""",
                )
            }

            val yrkesaktivitetId =
                client
                    .get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<YrkesaktivitetDTO>>()
                    .first()
                    .id

            // Oppdater perioder med ARBEIDSGIVERPERIODE
            val perioder = """{
                "type": "ARBEIDSGIVERPERIODE",
                "perioder": [
                    {
                        "fom": "2023-01-01",
                        "tom": "2023-01-15"
                    },
                    {
                        "fom": "2023-01-20",
                        "tom": "2023-01-31"
                    }
                ]
            }"""

            val response =
                client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$yrkesaktivitetId/perioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(perioder)
                }
            assertEquals(HttpStatusCode.NoContent, response.status)

            // Verifiser at perioder ble lagret
            val oppdatertYrkesaktivitet = daoer.yrkesaktivitetDao.hentYrkesaktivitetDbRecord(yrkesaktivitetId)!!
            assertTrue(oppdatertYrkesaktivitet.perioder != null)
            assertEquals("ARBEIDSGIVERPERIODE", oppdatertYrkesaktivitet.perioder!!.type.name)
            assertEquals(2, oppdatertYrkesaktivitet.perioder!!.perioder.size)
            assertEquals(
                "2023-01-01",
                oppdatertYrkesaktivitet.perioder!!
                    .perioder[0]
                    .fom
                    .toString(),
            )
            assertEquals(
                "2023-01-15",
                oppdatertYrkesaktivitet.perioder!!
                    .perioder[0]
                    .tom
                    .toString(),
            )

            // Oppdater perioder med VENTETID
            val ventetidPerioder = """{
                "type": "VENTETID",
                "perioder": [
                    {
                        "fom": "2023-01-16",
                        "tom": "2023-01-19"
                    }
                ]
            }"""

            client
                .put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$yrkesaktivitetId/perioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(ventetidPerioder)
                }.let { response ->
                    assertEquals(HttpStatusCode.NoContent, response.status)
                }

            // Verifiser at perioder ble oppdatert
            val oppdatertYrkesaktivitet2 = daoer.yrkesaktivitetDao.hentYrkesaktivitetDbRecord(yrkesaktivitetId)!!
            assertEquals("VENTETID", oppdatertYrkesaktivitet2.perioder!!.type.name)
            assertEquals(1, oppdatertYrkesaktivitet2.perioder!!.perioder.size)

            // Slett perioder ved å sende null
            client
                .put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$yrkesaktivitetId/perioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody("null")
                }.let { response ->
                    assertEquals(HttpStatusCode.NoContent, response.status)
                }

            // Verifiser at perioder ble slettet
            val oppdatertYrkesaktivitet3 = daoer.yrkesaktivitetDao.hentYrkesaktivitetDbRecord(yrkesaktivitetId)!!
            assertEquals(null, oppdatertYrkesaktivitet3.perioder)
        }
    }
}
