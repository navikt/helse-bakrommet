package no.nav.helse.bakrommet.e2e.behandling

import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.aareg.arbeidsforhold
import no.nav.helse.bakrommet.aareg.fastAnsettelse
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.etInntektSvar
import no.nav.helse.bakrommet.api.dto.dokumenter.DokumentDto
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.domain.enAktørId
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.sigrun.client2010to2050
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertIs

class DokumentHentingTest {
    private val naturligIdent = enNaturligIdent()

    @Test
    fun `henter ainntekt dokument`() {
        val fakeInntektForFnrRespons = etInntektSvar()

        runApplicationTest(
            aInntektClient = AInntektMock.aInntektClientMock(fnrTilAInntektResponse = mapOf(naturligIdent.value to fakeInntektForFnrRespons)),
        ) {
            val personPseudoId = personsøk(naturligIdent)

            // Opprett saksbehandlingsperiode via action
            val behandling =
                opprettBehandling(
                    personPseudoId,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent ainntekt dokument

            val postResponse = hentAInntektDokument(personPseudoId, behandling.id)
            assertIs<ApiResult.Success<DokumentDto>>(postResponse)
            val getResponse = hentDokument(personPseudoId, behandling.id, postResponse.response.id)
            assertEquals(postResponse.response, getResponse)
        }
    }

    @Test
    fun `403 fra Inntektskomponenten gir 403 videre med feilbeskrivelse`() =
        runApplicationTest(
            aInntektClient =
                AInntektMock.aInntektClientMock(fnrTilAInntektResponse = emptyMap(), forbiddenFødselsnumre = listOf(naturligIdent.value)),
        ) {
            val personPseudoId = personsøk(naturligIdent)

            // Opprett saksbehandlingsperiode via action
            val behandling =
                opprettBehandling(
                    personPseudoId,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent ainntekt dokument
            val result = hentAInntektDokument(personPseudoId, behandling.id)
            assertIs<ApiResult.Error>(result)
            assertEquals("Ingen tilgang", result.problemDetails.title)
            assertEquals("Ikke tilstrekkelig tilgang i A-Inntekt", result.problemDetails.detail)
            assertEquals(403, result.problemDetails.status)
        }

    @Test
    fun `henter arbeidsforhold dokument`() {
        val fakeAARegForFnrRespons =
            listOf(
                arbeidsforhold(
                    fnr = naturligIdent.value,
                    orgnummer = etOrganisasjonsnummer(),
                    startdato = LocalDate.parse("2014-01-01"),
                    stillingsprosent = 100.0,
                    ansettelsesform = fastAnsettelse(),
                    navArbeidsforholdId = 12345,
                ) {
                    aktorId(enAktørId())
                    opplysningspliktig(etOrganisasjonsnummer())
                    yrke("1231119", "KONTORLEDER")
                    arbeidstidsordning("ikkeSkift", "Ikke skift")
                    rapporteringsmaaneder("2019-11", null)
                },
            )

        val forventetJsonNode = objectMapper.writeValueAsString(fakeAARegForFnrRespons).asJsonNode()

        runApplicationTest(
            aaRegClient = AARegMock.aaRegClientMock(fnrTilArbeidsforhold = mapOf(naturligIdent.value to fakeAARegForFnrRespons)),
        ) {
            val personPseudoId = personsøk(naturligIdent)

            // Opprett saksbehandlingsperiode via action
            val behandling =
                opprettBehandling(
                    personPseudoId,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent arbeidsforhold dokument
            val postResponse = hentArbeidsforholdDokument(personPseudoId, behandling.id)
            assertIs<ApiResult.Success<DokumentDto>>(postResponse)
            assertEquals(forventetJsonNode, postResponse.response.innhold)
            val getResponse = hentDokument(personPseudoId, behandling.id, postResponse.response.id)
            assertEquals(postResponse.response, getResponse)
        }
    }

    @Test
    fun `403 fra AA-reg gir 403 videre med feilbeskrivelse`() =
        runApplicationTest(
            aaRegClient = AARegMock.aaRegClientMock(forbiddenFødselsnumre = listOf(naturligIdent.value)),
        ) {
            val personPseudoIdForbidden = personsøk(naturligIdent)

            // Opprett saksbehandlingsperiode via action
            val behandling =
                opprettBehandling(
                    personPseudoIdForbidden.toString(),
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent arbeidsforhold dokument
            val response = hentArbeidsforholdDokument(personPseudoIdForbidden, behandling.id)
            assertIs<ApiResult.Error>(response)
            assertEquals("Ingen tilgang", response.problemDetails.title)
            assertEquals("Ikke tilstrekkelig tilgang i AA-REG", response.problemDetails.detail)
            assertEquals(403, response.problemDetails.status)
        }

    @Test
    fun `henter pensjonsgivende inntekt dokument`() {
        runApplicationTest(
            pensjonsgivendeInntektProvider = client2010to2050(naturligIdent.value),
        ) {
            val personPseudoId = personsøk(naturligIdent)

            // Opprett saksbehandlingsperiode via action
            val behandling =
                opprettBehandling(
                    personPseudoId,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )

            // Hent pensjonsgivendeinntekt dokument
            val postResponse = hentPersonsgivendeInntektDokument(personPseudoId, behandling.id)
            assertIs<ApiResult.Success<DokumentDto>>(postResponse)
            assertEquals(
                setOf(2022, 2021, 2020),
                postResponse.response.innhold
                    .map { it["inntektsaar"].asInt() }
                    .toSet(),
            )
            val getResponse = hentDokument(personPseudoId, behandling.id, postResponse.response.id)
            assertEquals(postResponse.response, getResponse)
        }
    }
}
