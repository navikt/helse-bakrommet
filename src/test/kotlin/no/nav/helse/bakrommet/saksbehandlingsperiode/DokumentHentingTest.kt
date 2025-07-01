package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.etInntektSvar
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.sigrun.SigrunClientTest
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DokumentHentingTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `henter ainntekt dokument`() {
        val fakeInntektForFnrRespons = etInntektSvar(fnr = FNR)

        runApplicationTest(
            aInntektClient = AInntektMock.aInntektClientMock(mapOf(FNR to fakeInntektForFnrRespons)),
        ) { daoer ->
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

            // Hent ainntekt dokument
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/dokumenter/ainntekt/hent") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{ "fom" : "2024-05", "tom" : "2025-06" }""")
            }.let { postResponse ->
                val location = postResponse.headers["Location"]!!
                val jsonPostResponse = postResponse.body<DokumentDto>()
                assertEquals("ainntekt828", jsonPostResponse.dokumentType)

                assertEquals(201, postResponse.status.value)
                assertEquals(fakeInntektForFnrRespons.asJsonNode(), jsonPostResponse.innhold)

                // Verifiser at dokumentet kan hentes via location-header
                client.get(location) {
                    bearerAuth(TestOppsett.userToken)
                }.let { getResponse ->
                    assertEquals(200, getResponse.status.value)
                    val jsonGetResponse = getResponse.body<DokumentDto>()
                    assertEquals(jsonPostResponse, jsonGetResponse)
                }
            }
        }
    }

    @Test
    fun `henter arbeidsforhold dokument`() {
        val fakeAARegForFnrRespons = AARegMock.Person1.respV2

        runApplicationTest(
            aaRegClient = AARegMock.aaRegClientMock(mapOf(FNR to fakeAARegForFnrRespons)),
        ) { daoer ->
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

            // Hent arbeidsforhold dokument
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/dokumenter/arbeidsforhold/hent") {
                bearerAuth(TestOppsett.userToken)
            }.let { postResponse ->
                val location = postResponse.headers["Location"]!!
                val jsonPostResponse = postResponse.body<DokumentDto>()
                assertEquals("arbeidsforhold", jsonPostResponse.dokumentType)

                assertEquals(201, postResponse.status.value)
                assertEquals(fakeAARegForFnrRespons.asJsonNode(), jsonPostResponse.innhold)

                // Verifiser at dokumentet kan hentes via location-header
                client.get(location) {
                    bearerAuth(TestOppsett.userToken)
                }.let { getResponse ->
                    assertEquals(200, getResponse.status.value)
                    val jsonGetResponse = getResponse.body<DokumentDto>()
                    assertEquals(jsonPostResponse, jsonGetResponse)
                }
            }
        }
    }

    @Test
    fun `henter pensjonsgivende inntekt dokument`() {
        runApplicationTest(
            sigrunClient = SigrunClientTest.client2010to2050(FNR),
        ) { daoer ->
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

            // Hent pensjonsgivendeinntekt dokument
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/dokumenter/pensjonsgivendeinntekt/hent") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{ "senesteÅrTom": 2025, "antallÅrBakover": 3 }""")
            }.let { postResponse ->
                val location = postResponse.headers["Location"]!!
                val jsonPostResponse = postResponse.body<DokumentDto>()
                assertEquals("pensjonsgivendeinntekt", jsonPostResponse.dokumentType)

                assertEquals(201, postResponse.status.value)

                val data = jsonPostResponse.innhold
                assertEquals(setOf(2023, 2024, 2025), data.map { it["inntektsaar"].asInt() }.toSet())

                // Verifiser at dokumentet kan hentes via location-header
                client.get(location) {
                    bearerAuth(TestOppsett.userToken)
                }.let { getResponse ->
                    assertEquals(200, getResponse.status.value)
                    val jsonGetResponse = getResponse.body<DokumentDto>()
                    assertEquals(jsonPostResponse, jsonGetResponse)
                }
            }
        }
    }
}
