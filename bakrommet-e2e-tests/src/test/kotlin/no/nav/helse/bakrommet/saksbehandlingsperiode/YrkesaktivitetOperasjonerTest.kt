package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.inntektsmeldingMockHttpClient
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDTO
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.TypeArbeidstaker
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.hentDekningsgrad
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettYrkesaktivitet
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class YrkesaktivitetOperasjonerTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `oppdaterer dagoversikt for yrkesaktivitet`() {
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

            opprettYrkesaktivitet(
                personId = PERSON_ID,
                periode.id,
                YrkesaktivitetKategorisering.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                ),
            )

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
    fun `oppdaterer dagoversikt for yrkesaktivitet med nytt format`() {
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

            opprettYrkesaktivitet(
                personId = PERSON_ID,
                periode.id,
                YrkesaktivitetKategorisering.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                ),
            )

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
    fun `oppdaterer perioder for yrkesaktivitet`() {
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

            opprettYrkesaktivitet(
                personId = PERSON_ID,
                periode.id,
                YrkesaktivitetKategorisering.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                ),
            )

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

    @Test
    fun `henter inntektsmeldinger, og velger en av de, for yrkesaktivitet`() {
        val im1Id = UUID.randomUUID().toString()
        val im2Id = UUID.randomUUID().toString()
        val imFeilPersonId = UUID.randomUUID().toString()
        val im1 = skapInntektsmelding(arbeidstakerFnr = FNR, inntektsmeldingId = im1Id)
        val im2 = skapInntektsmelding(arbeidstakerFnr = FNR, inntektsmeldingId = im2Id)
        val imFeilPerson = skapInntektsmelding(arbeidstakerFnr = "00000011111", inntektsmeldingId = imFeilPersonId)
        val antallKallTilInntektsmeldingAPI = AtomicInteger(0)
        runApplicationTest(
            inntektsmeldingClient =
                InntektsmeldingApiMock.inntektsmeldingClientMock(
                    mockClient =
                        inntektsmeldingMockHttpClient(
                            fnrTilInntektsmeldinger =
                                mapOf(
                                    FNR to listOf(im1, im2),
                                ),
                            callCounter = antallKallTilInntektsmeldingAPI,
                        ),
                ),
        ) { daoer ->
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

            // Sett skjæringstidspunkt for perioden
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/skjaeringstidspunkt") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{ "skjaeringstidspunkt": "2023-01-15" }""")
            }

            // Opprett yrkesaktivitet
            opprettYrkesaktivitet(
                personId = PERSON_ID,
                periode.id,
                YrkesaktivitetKategorisering.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                ),
            )

            val yrkesaktivitetId =
                client
                    .get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<YrkesaktivitetDTO>>()
                    .first()
                    .id

            // Hent inntektsmeldinger for yrkesaktivitet
            val response =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$yrkesaktivitetId/inntektsmeldinger") {
                    bearerAuth(TestOppsett.userToken)
                }

            assertEquals(HttpStatusCode.OK, response.status)

            // Verifiser at responsen er en gyldig JSON-array
            val inntektsmeldinger = response.body<JsonNode>()
            assertTrue(inntektsmeldinger.isArray, "Responsen skal være en JSON-array")
            assertEquals("[${im1.serialisertTilString()},${im2.serialisertTilString()}]".asJsonNode(), inntektsmeldinger)

            suspend fun velgIm(inntektsmeldingId: String) =
                client.put("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    val req =
                        InntektRequest
                            .Arbeidstaker(
                                data =
                                    ArbeidstakerInntektRequest.Inntektsmelding(
                                        inntektsmeldingId = inntektsmeldingId,
                                        begrunnelse = "derfor",
                                        refusjon = listOf(),
                                    ),
                            ).serialisertTilString()
                    setBody(req)
                }
            velgIm(im2Id).apply {
                assertEquals(HttpStatusCode.NoContent, status)
            }
            assertEquals(2, antallKallTilInntektsmeldingAPI.get())

            // TODO: Sjekk også at grunnlaget blir satt?

            fun finnImDok(inntektsmeldingId: String) = daoer.dokumentDao.finnDokumentMedEksternId(periode.id, "inntektsmelding", inntektsmeldingId)

            assertNull(finnImDok(im1Id), "im1 skal ikke ha blitt lagret, siden den ikke er brukt til noe")
            assertEquals(
                im2.serialisertTilString().asJsonNode(),
                finnImDok(im2Id)!!
                    .innhold
                    .asJsonNode(),
            )

            velgIm(im2Id).apply {
                assertEquals(HttpStatusCode.NoContent, status)
            }
            assertEquals(2, antallKallTilInntektsmeldingAPI.get(), "Forespørsel om allerede lagret IM skal ikke gi nytt API-kall")

            velgIm(imFeilPersonId).apply {
                assertEquals(
                    HttpStatusCode.InternalServerError,
                    status,
                    "Skal feile hvis FNR ikke stemmer overens",
                )
            }
            assertEquals(3, antallKallTilInntektsmeldingAPI.get())
            assertNull(
                finnImDok(imFeilPersonId),
                "Skal ikke lagre inntektsmelding som ikke stemmer",
            )
        }
    }

    @Test
    fun `henter inntektsmeldinger feiler når skjæringstidspunkt ikke er satt`() {
        runApplicationTest { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett saksbehandlingsperiode uten skjæringstidspunkt
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

            opprettYrkesaktivitet(
                personId = PERSON_ID,
                periode.id,
                YrkesaktivitetKategorisering.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                ),
            )

            val yrkesaktivitetId =
                client
                    .get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<YrkesaktivitetDTO>>()
                    .first()
                    .id

            // Hent inntektsmeldinger for yrkesaktivitet
            val response =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet/$yrkesaktivitetId/inntektsmeldinger") {
                    bearerAuth(TestOppsett.userToken)
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val errorResponse = response.bodyAsText()
            errorResponse `should equal` "[]"
        }
    }
}
