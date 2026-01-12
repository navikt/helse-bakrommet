package no.nav.helse.bakrommet.behandling

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.aareg.arbeidsforhold
import no.nav.helse.bakrommet.aareg.fastAnsettelse
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.etInntektSvar
import no.nav.helse.bakrommet.api.dto.dokumenter.DokumentDto
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.sigrun.client2010to2050
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettBehandling
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DokumentHentingTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
        val PERSON_PSEUDO_ID = UUID.nameUUIDFromBytes(PERSON_ID.toByteArray())
    }

    @Test
    fun `henter ainntekt dokument`() {
        val fakeInntektForFnrRespons = etInntektSvar()

        runApplicationTest(
            aInntektClient = AInntektMock.aInntektClientMock(fnrTilAInntektResponse = mapOf(FNR to fakeInntektForFnrRespons)),
        ) { daoer ->
            daoer.personPseudoIdDao.opprettPseudoId(PERSON_PSEUDO_ID, NaturligIdent(FNR))

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettBehandling(
                    PERSON_PSEUDO_ID,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent ainntekt dokument
            client
                .post("/v1/${PERSON_PSEUDO_ID}/behandlinger/${periode.id}/dokumenter/ainntekt/hent-8-28") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody("""{ "fom" : "2022-08", "tom" : "2022-11" }""")
                }.let { postResponse ->
                    val location = postResponse.headers["Location"]!!
                    val jsonPostResponse = postResponse.body<DokumentDto>()
                    assertEquals("ainntekt828", jsonPostResponse.dokumentType)

                    assertEquals(201, postResponse.status.value)
                    // Mocken filtrerer og konverterer til A-Inntekt API format, så vi sjekker at innholdet er en gyldig JsonNode

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
                AInntektMock.aInntektClientMock(fnrTilAInntektResponse = emptyMap()),
        ) {
            val personIdForbidden = "ab403"
            val personPseudoIdForbidden = UUID.nameUUIDFromBytes(personIdForbidden.toByteArray())
            it.personPseudoIdDao.opprettPseudoId(personPseudoIdForbidden, NaturligIdent("01019000" + "403"))

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettBehandling(
                    personPseudoIdForbidden.toString(),
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent ainntekt dokument
            client
                .post("/v1/$personPseudoIdForbidden/behandlinger/${periode.id}/dokumenter/ainntekt/hent-8-28") {
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
        val fakeAARegForFnrRespons =
            listOf(
                arbeidsforhold(
                    fnr = FNR,
                    orgnummer = "999444333",
                    startdato = LocalDate.parse("2014-01-01"),
                    stillingsprosent = 100.0,
                    ansettelsesform = fastAnsettelse(),
                    navArbeidsforholdId = 12345,
                ) {
                    aktorId("1111122222333")
                    opplysningspliktig("888777666")
                    yrke("1231119", "KONTORLEDER")
                    arbeidstidsordning("ikkeSkift", "Ikke skift")
                    rapporteringsmaaneder("2019-11", null)
                },
            )

        val forventetJsonNode = objectMapper.writeValueAsString(fakeAARegForFnrRespons).asJsonNode()

        runApplicationTest(
            aaRegClient = AARegMock.aaRegClientMock(fnrTilArbeidsforhold = mapOf(FNR to fakeAARegForFnrRespons)),
        ) { daoer ->
            daoer.personPseudoIdDao.opprettPseudoId(PERSON_PSEUDO_ID, NaturligIdent(FNR))

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettBehandling(
                    PERSON_PSEUDO_ID,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent arbeidsforhold dokument
            client
                .post("/v1/${PERSON_PSEUDO_ID}/behandlinger/${periode.id}/dokumenter/arbeidsforhold/hent") {
                    bearerAuth(TestOppsett.userToken)
                }.let { postResponse ->
                    val location = postResponse.headers["Location"]!!
                    val jsonPostResponse = postResponse.body<DokumentDto>()
                    assertEquals("arbeidsforhold", jsonPostResponse.dokumentType)

                    assertEquals(201, postResponse.status.value)
                    assertEquals(forventetJsonNode, jsonPostResponse.innhold)

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
            val personPseudoIdForbidden = UUID.nameUUIDFromBytes(personIdForbidden.toByteArray())
            it.personPseudoIdDao.opprettPseudoId(personPseudoIdForbidden, NaturligIdent("01019000" + "403"))

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettBehandling(
                    personPseudoIdForbidden.toString(),
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent arbeidsforhold dokument
            client
                .post("/v1/$personPseudoIdForbidden/behandlinger/${periode.id}/dokumenter/arbeidsforhold/hent") {
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
            pensjonsgivendeInntektProvider = client2010to2050(FNR),
        ) { daoer ->
            daoer.personPseudoIdDao.opprettPseudoId(PERSON_PSEUDO_ID, NaturligIdent(FNR))

            // Opprett saksbehandlingsperiode via action
            val periode =
                opprettBehandling(
                    PERSON_PSEUDO_ID,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent pensjonsgivendeinntekt dokument
            client
                .post("/v1/${PERSON_PSEUDO_ID}/behandlinger/${periode.id}/dokumenter/pensjonsgivendeinntekt/hent") {
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
