package no.nav.helse.bakrommet.e2e.behandling

import io.ktor.http.HttpStatusCode
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.inntektsmeldingMockHttpClient
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.januar
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import java.time.LocalDate
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YrkesaktivitetOperasjonerTest {
    private val naturligIdent = enNaturligIdent()

    @Test
    fun `oppdaterer dagoversikt for yrkesaktivitet`() {
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            opprettBehandling(personPseudoId, 1.januar(2023), 10.januar(2023))

            val behandling =
                hentBehandlingerForPerson(personPseudoId)
                    .single()

            val yrkesaktivitetId =
                opprettYrkesaktivitetOld(
                    personId = personPseudoId,
                    behandling.id,
                    YrkesaktivitetKategoriseringDto.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = etOrganisasjonsnummer()),
                    ),
                )

            val oppdateringer =
                listOf(
                    DagDto(2.januar(2023), DagtypeDto.Syk, 100, listOf(), kilde = null),
                    DagDto(3.januar(2023), DagtypeDto.Arbeidsdag, 0, listOf(), kilde = null),
                    DagDto(7.januar(2023), DagtypeDto.Syk, 50, listOf(), kilde = null),
                )

            settDagoversikt(personPseudoId, behandling.id, yrkesaktivitetId, oppdateringer)
            val yrkesaktiviteter = hentYrkesaktiviteter(personPseudoId, behandling.id)
            assertEquals(1, yrkesaktiviteter.size)
            val dagoversikt = yrkesaktiviteter.single().dagoversikt
            assertNotNull(dagoversikt)

            // Verifiser at dagoversikten er oppdatert korrekt
            assertTrue(dagoversikt.isNotEmpty())

            assertEquals(KildeDto.Saksbehandler, dagoversikt.single { it.dato.isEqual(2.januar(2023)) }.kilde)
            assertEquals(KildeDto.Saksbehandler, dagoversikt.single { it.dato.isEqual(3.januar(2023)) }.kilde)
            assertEquals(KildeDto.Saksbehandler, dagoversikt.single { it.dato.isEqual(7.januar(2023)) }.kilde)
        }
    }

    @Test
    fun `oppdaterer kategorisering for yrkesaktivitet`() {
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            opprettBehandling(personPseudoId, 1.januar(2023), 10.januar(2023))

            val behandling =
                hentBehandlingerForPerson(personPseudoId)
                    .single()

            val yrkesaktivitetId =
                opprettYrkesaktivitetOld(
                    personId = personPseudoId,
                    behandling.id,
                    YrkesaktivitetKategoriseringDto.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = etOrganisasjonsnummer()),
                    ),
                )

            val nyttOrganisasjonsnummer = etOrganisasjonsnummer()
            oppdaterKategorisering(
                personId = personPseudoId,
                behandlingId = behandling.id,
                yrkesaktivitetId = yrkesaktivitetId,
                kategorisering =
                    YrkesaktivitetKategoriseringDto.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = nyttOrganisasjonsnummer),
                    ),
            )

            val yrkesaktiviteter = hentYrkesaktiviteter(personPseudoId, behandling.id)
            assertEquals(1, yrkesaktiviteter.size)
            val yrkesaktivitet = yrkesaktiviteter.single()
            assertEquals(YrkesaktivitetKategoriseringDto.Arbeidstaker(true, TypeArbeidstakerDto.Ordinær(nyttOrganisasjonsnummer)), yrkesaktivitet.kategorisering)
        }
    }

    @Test
    fun `kan ikke oppgi avslagsdag uten begrunnelse`() {
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            opprettBehandling(personPseudoId, 1.januar(2023), 10.januar(2023))

            val behandling =
                hentBehandlingerForPerson(personPseudoId)
                    .single()

            val yrkesaktivitetId =
                opprettYrkesaktivitetOld(
                    personId = personPseudoId,
                    behandling.id,
                    YrkesaktivitetKategoriseringDto.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = etOrganisasjonsnummer()),
                    ),
                )

            // APIet gir feil hvis man sender en avslått dag uten begrunnelse
            val result =
                settDagoversikt(
                    personId = personPseudoId,
                    behandlingId = behandling.id,
                    yrkesaktivitetId = yrkesaktivitetId,
                    dager =
                        listOf(
                            DagDto(
                                dato = LocalDate.of(2023, 1, 1),
                                dagtype = DagtypeDto.Avslått,
                                grad = null,
                                kilde = null,
                                avslåttBegrunnelse = listOf(),
                            ),
                        ),
                )
            assertIs<ApiResult.Error>(result)
            assertEquals("Avslåtte dager må ha avslagsgrunn", result.problemDetails.title)
            assertEquals(null, result.problemDetails.detail)
            assertEquals(400, result.problemDetails.status)
        }
    }

    @Test
    fun `legg til arbeidsgiverperioder for yrkesaktivitet`() {
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            // Opprett saksbehandlingsperiode
            opprettBehandling(personPseudoId, 1.januar(2023), 10.januar(2023))

            val behandling =
                hentBehandlingerForPerson(personPseudoId)
                    .single()

            opprettYrkesaktivitetOld(
                personId = personPseudoId,
                behandling.id,
                YrkesaktivitetKategoriseringDto.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = etOrganisasjonsnummer()),
                ),
            )

            val yrkesaktivitet =
                hentYrkesaktiviteter(personPseudoId, behandling.id)
                    .single()

            // Oppdater perioder med ARBEIDSGIVERPERIODE
            val arbeidsgiverperioder =
                PerioderDto(
                    type = PeriodetypeDto.ARBEIDSGIVERPERIODE,
                    perioder =
                        listOf(
                            PeriodeDto(
                                fom = 1.januar(2023),
                                tom = 15.januar(2023),
                            ),
                            PeriodeDto(
                                fom = 20.januar(2023),
                                tom = 31.januar(2023),
                            ),
                        ),
                )

            oppdaterYrkesaktivitetsperioder(personPseudoId, behandling.id, yrkesaktivitet.id, arbeidsgiverperioder)

            // Verifiser at perioder ble lagret
            val oppdatertYrkesaktivitet =
                hentYrkesaktiviteter(personPseudoId, behandling.id)
                    .single()
            val oppdaterteArbeidsgiverperioder = oppdatertYrkesaktivitet.perioder
            assertNotNull(oppdaterteArbeidsgiverperioder)
            assertEquals(PeriodetypeDto.ARBEIDSGIVERPERIODE, oppdaterteArbeidsgiverperioder.type)
            assertEquals(2, oppdaterteArbeidsgiverperioder.perioder.size)
            assertEquals(1.januar(2023), oppdaterteArbeidsgiverperioder.perioder[0].fom)
            assertEquals(15.januar(2023), oppdaterteArbeidsgiverperioder.perioder[0].tom)
            assertEquals(20.januar(2023), oppdaterteArbeidsgiverperioder.perioder[1].fom)
            assertEquals(31.januar(2023), oppdaterteArbeidsgiverperioder.perioder[1].tom)
        }
    }

    @Test
    fun `legg til ventetidsperioder for yrkesaktivitet`() {
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            // Opprett saksbehandlingsperiode
            opprettBehandling(personPseudoId, 1.januar(2023), 10.januar(2023))

            val behandling =
                hentBehandlingerForPerson(personPseudoId)
                    .single()

            opprettYrkesaktivitetOld(
                personId = personPseudoId,
                behandling.id,
                YrkesaktivitetKategoriseringDto.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = etOrganisasjonsnummer()),
                ),
            )

            val yrkesaktivitet =
                hentYrkesaktiviteter(personPseudoId, behandling.id)
                    .single()

            // Oppdater perioder med ARBEIDSGIVERPERIODE
            val ventetidsperioder =
                PerioderDto(
                    type = PeriodetypeDto.VENTETID,
                    perioder =
                        listOf(
                            PeriodeDto(
                                fom = 1.januar(2023),
                                tom = 15.januar(2023),
                            ),
                            PeriodeDto(
                                fom = 20.januar(2023),
                                tom = 31.januar(2023),
                            ),
                        ),
                )

            oppdaterYrkesaktivitetsperioder(personPseudoId, behandling.id, yrkesaktivitet.id, ventetidsperioder)

            // Verifiser at perioder ble lagret
            val oppdatertYrkesaktivitet =
                hentYrkesaktiviteter(personPseudoId, behandling.id)
                    .single()
            val oppdaterteArbeidsgiverperioder = oppdatertYrkesaktivitet.perioder
            assertNotNull(oppdaterteArbeidsgiverperioder)
            assertEquals(PeriodetypeDto.VENTETID, oppdaterteArbeidsgiverperioder.type)
            assertEquals(2, oppdaterteArbeidsgiverperioder.perioder.size)
            assertEquals(1.januar(2023), oppdaterteArbeidsgiverperioder.perioder[0].fom)
            assertEquals(15.januar(2023), oppdaterteArbeidsgiverperioder.perioder[0].tom)
            assertEquals(20.januar(2023), oppdaterteArbeidsgiverperioder.perioder[1].fom)
            assertEquals(31.januar(2023), oppdaterteArbeidsgiverperioder.perioder[1].tom)
        }
    }

    @Test
    fun `slett perioder for yrkesaktivitet`() {
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            // Opprett saksbehandlingsperiode
            opprettBehandling(personPseudoId, 1.januar(2023), 10.januar(2023))

            val behandling =
                hentBehandlingerForPerson(personPseudoId)
                    .single()

            opprettYrkesaktivitetOld(
                personId = personPseudoId,
                behandling.id,
                YrkesaktivitetKategoriseringDto.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = etOrganisasjonsnummer()),
                ),
            )

            val yrkesaktivitet =
                hentYrkesaktiviteter(personPseudoId, behandling.id)
                    .single()

            val arbeidsgiverperioder =
                PerioderDto(
                    type = PeriodetypeDto.ARBEIDSGIVERPERIODE,
                    perioder =
                        listOf(
                            PeriodeDto(
                                fom = 1.januar(2023),
                                tom = 15.januar(2023),
                            ),
                        ),
                )

            oppdaterYrkesaktivitetsperioder(personPseudoId, behandling.id, yrkesaktivitet.id, arbeidsgiverperioder)

            // Slett perioder ved å sende null
            slettYrkesaktivitetsperioder(personPseudoId, behandling.id, yrkesaktivitet.id)

            // Verifiser at perioder ble slettet
            val oppdatertYrkesaktivitet =
                hentYrkesaktiviteter(personPseudoId, behandling.id)
                    .single()
            assertEquals(null, oppdatertYrkesaktivitet.perioder)
        }
    }

    @Test
    fun `henter inntektsmeldinger, og velger en av de, for yrkesaktivitet`() {
        val im1Id = UUID.randomUUID().toString()
        val im2Id = UUID.randomUUID().toString()
        val imFeilPersonId = UUID.randomUUID().toString()
        val organisasjonsnummer = etOrganisasjonsnummer()
        val fomBehandling = 1.januar(2023)
        val im1 = skapInntektsmelding(arbeidstakerFnr = naturligIdent.value, inntektsmeldingId = im1Id, virksomhetsnummer = organisasjonsnummer, foersteFravaersdag = fomBehandling)
        val im2 = skapInntektsmelding(arbeidstakerFnr = naturligIdent.value, inntektsmeldingId = im2Id, virksomhetsnummer = organisasjonsnummer, foersteFravaersdag = fomBehandling)
        val antallKallTilInntektsmeldingAPI = AtomicInteger(0)
        runApplicationTest(
            inntektsmeldingClient =
                InntektsmeldingApiMock.inntektsmeldingClientMock(
                    mockClient =
                        inntektsmeldingMockHttpClient(
                            fnrTilInntektsmeldinger =
                                mapOf(
                                    naturligIdent.value to listOf(im1, im2),
                                ),
                            callCounter = antallKallTilInntektsmeldingAPI,
                        ),
                ),
        ) {
            val personPseudoId = personsøk(naturligIdent)

            // Opprett saksbehandlingsperiode
            opprettBehandling(personPseudoId, fomBehandling, 31.januar(2023))

            val behandling = hentBehandlingerForPerson(personPseudoId).single()

            // Sett skjæringstidspunkt for perioden
            oppdaterSkjæringstidspunkt(personPseudoId, behandling.id, 15.januar(2023))

            // Opprett yrkesaktivitet
            opprettYrkesaktivitetOld(
                personId = personPseudoId,
                behandling.id,
                YrkesaktivitetKategoriseringDto.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = organisasjonsnummer),
                ),
            )

            val yrkesaktivitet = hentYrkesaktiviteter(personPseudoId, behandling.id).single()

            // Hent inntektsmeldinger for yrkesaktivitet
            val result = hentInntektsmeldinger(personPseudoId, behandling.id, yrkesaktivitet.id)
            assertIs<ApiResult.Success<List<Inntektsmelding>>>(result)

            saksbehandlerVelgerInntektsmelding(personPseudoId, behandling.id, yrkesaktivitet.id, result.response[0].inntektsmeldingId)

            val dokumenter =
                hentDokumenter(personPseudoId, behandling.id)
                    .filter { it.dokumentType == "inntektsmelding" }
            assertEquals(1, dokumenter.size)

            val hentImForAnnenPersonResult = saksbehandlerVelgerInntektsmelding(personPseudoId, behandling.id, yrkesaktivitet.id, imFeilPersonId)
            assertIs<ApiResult.Error>(hentImForAnnenPersonResult)
            assertEquals(HttpStatusCode.InternalServerError.value, hentImForAnnenPersonResult.problemDetails.status)
            assertEquals("Ukjent feil", hentImForAnnenPersonResult.problemDetails.title)
            assertNotNull(hentImForAnnenPersonResult.problemDetails.detail)
        }
    }

    @Test
    fun `Får 404 hvis man oppretter yrkesaktivitet på ikke eksisterende orgnummer`() {
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            val behandling =
                opprettBehandling(personPseudoId, 1.januar(2023), 31.januar(2023))

            val opprettYrkesaktivitetResult =
                opprettYrkesaktivitet(
                    personId = personPseudoId,
                    behandlingId = behandling.id,
                    kategorisering =
                        YrkesaktivitetKategoriseringDto.Arbeidstaker(
                            sykmeldt = true,
                            typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = "123"),
                        ),
                )
            assertIs<ApiResult.Error>(opprettYrkesaktivitetResult)
            assertEquals(HttpStatusCode.NotFound.value, opprettYrkesaktivitetResult.problemDetails.status)
            assertEquals("Organisasjon ikke funnet", opprettYrkesaktivitetResult.problemDetails.title)
            assertEquals("Fant ikke organisasjon i EREG for organisasjonsnummer 123", opprettYrkesaktivitetResult.problemDetails.detail)
        }
    }

    @Test
    fun `Får 404 hvis man oppdaterer yrkesaktivitet til et ikke-eksisterende orgnummer`() {
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            val behandling =
                opprettBehandling(personPseudoId, 1.januar(2023), 31.januar(2023))

            val opprettYrkesaktivitetResult =
                opprettYrkesaktivitet(
                    personPseudoId,
                    behandling.id,
                    YrkesaktivitetKategoriseringDto.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = etOrganisasjonsnummer()),
                    ),
                )
            assertIs<ApiResult.Success<YrkesaktivitetDto>>(opprettYrkesaktivitetResult)
            val oppdaterResult =
                oppdaterKategorisering(
                    personId = personPseudoId,
                    behandlingId = behandling.id,
                    yrkesaktivitetId = opprettYrkesaktivitetResult.response.id,
                    kategorisering =
                        YrkesaktivitetKategoriseringDto.Arbeidstaker(
                            sykmeldt = true,
                            typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = "123"),
                        ),
                )
            assertIs<ApiResult.Error>(oppdaterResult)
            assertEquals(HttpStatusCode.NotFound.value, oppdaterResult.problemDetails.status)
            assertEquals("Organisasjon ikke funnet", oppdaterResult.problemDetails.title)
            assertEquals("Fant ikke organisasjon i EREG for organisasjonsnummer 123", oppdaterResult.problemDetails.detail)
        }
    }
}
