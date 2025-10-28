package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.etInntektSvar
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDto
import no.nav.helse.bakrommet.sigrun.SigrunClientTest
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettSaksbehandlingsperiode
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DokumentHentingTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `henter ainntekt dokument`() {
        val fakeInntektForFnrRespons = etInntektSvar(fnr = FNR)

        runApplicationTest(
            aInntektClient = AInntektMock.aInntektClientMock(fnrTilSvar = mapOf(FNR to fakeInntektForFnrRespons)),
        ) { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettSaksbehandlingsperiode(
                    PERSON_ID,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent ainntekt dokument
            client
                .post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/dokumenter/ainntekt/hent-8-28") {
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
                    client
                        .get(location) {
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
    fun `403 fra Inntektskomponenten gir 403 videre med feilbeskrivelse`() =
        runApplicationTest(
            aInntektClient =
                AInntektMock.aInntektClientMock(),
        ) {
            val personIdForbidden = "ab403"
            it.personDao.opprettPerson("01019000" + "403", personIdForbidden)

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettSaksbehandlingsperiode(
                    personIdForbidden,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent ainntekt dokument
            client
                .post("/v1/$personIdForbidden/saksbehandlingsperioder/${periode.id}/dokumenter/ainntekt/hent-8-28") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody("""{ "fom" : "2024-05", "tom" : "2025-06" }""")
                }.apply {
                    assertEquals(403, status.value)
                    assertEquals(
                        """
                    {"type":"about:blank","title":"Ingen tilgang","status":403,"detail":"Ikke tilstrekkelig tilgang i A-Inntekt","instance":null}
                """.asJsonNode(),
                        bodyAsText().asJsonNode(),
                    )
                }
        }

    @Test
    fun `henter arbeidsforhold dokument`() {
        val fakeAARegForFnrRespons = AARegMock.Person1.respV2

        runApplicationTest(
            aaRegClient = AARegMock.aaRegClientMock(fnrTilSvar = mapOf(FNR to fakeAARegForFnrRespons)),
        ) { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettSaksbehandlingsperiode(
                    PERSON_ID,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent arbeidsforhold dokument
            client
                .post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/dokumenter/arbeidsforhold/hent") {
                    bearerAuth(TestOppsett.userToken)
                }.let { postResponse ->
                    val location = postResponse.headers["Location"]!!
                    val jsonPostResponse = postResponse.body<DokumentDto>()
                    assertEquals("arbeidsforhold", jsonPostResponse.dokumentType)

                    assertEquals(201, postResponse.status.value)
                    assertEquals(fakeAARegForFnrRespons.asJsonNode(), jsonPostResponse.innhold)

                    // Verifiser at dokumentet kan hentes via location-header
                    client
                        .get(location) {
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
    fun `403 fra AA-reg gir 403 videre med feilbeskrivelse`() =
        runApplicationTest(
            aaRegClient = AARegMock.aaRegClientMock(),
        ) {
            val personIdForbidden = "ab403"
            it.personDao.opprettPerson("01019000" + "403", personIdForbidden)

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettSaksbehandlingsperiode(
                    personIdForbidden,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent arbeidsforhold dokument
            client
                .post("/v1/$personIdForbidden/saksbehandlingsperioder/${periode.id}/dokumenter/arbeidsforhold/hent") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(403, status.value)
                    assertEquals(
                        """
                    {"type":"about:blank","title":"Ingen tilgang","status":403,"detail":"Ikke tilstrekkelig tilgang i AA-REG","instance":null}
                """.asJsonNode(),
                        bodyAsText().asJsonNode(),
                    )
                }
        }

    @Test
    fun `henter pensjonsgivende inntekt dokument`() {
        runApplicationTest(
            sigrunClient = SigrunClientTest.client2010to2050(FNR),
        ) { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettSaksbehandlingsperiode(
                    PERSON_ID,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent pensjonsgivendeinntekt dokument
            client
                .post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/dokumenter/pensjonsgivendeinntekt/hent") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                }.let { postResponse ->
                    val location = postResponse.headers["Location"]!!
                    val jsonPostResponse = postResponse.body<DokumentDto>()
                    assertEquals("pensjonsgivendeinntekt", jsonPostResponse.dokumentType)

                    assertEquals(201, postResponse.status.value)

                    val data = jsonPostResponse.innhold
                    assertEquals(setOf(2022, 2021, 2020), data.map { it["inntektsaar"].asInt() }.toSet())

                    // Verifiser at dokumentet kan hentes via location-header
                    client
                        .get(location) {
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
