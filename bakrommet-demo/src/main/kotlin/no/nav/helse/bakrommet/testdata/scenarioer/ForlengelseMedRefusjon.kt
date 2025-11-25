package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.aareg.arbeidsforhold
import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
import no.nav.helse.bakrommet.behandling.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.ereg.betongbyggAS
import no.nav.helse.bakrommet.ereg.veihjelpenAS
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Saksbehandingsperiode
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Refusjon
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

private val fnr = "15089312342"
private val førsteArbeidsgiverOrgnummer = betongbyggAS.orgnummer
private val andreArbeidsgiverOrgnummer = veihjelpenAS.orgnummer

// Inntektsdata for første arbeidsforhold: 50000 kr/mnd
private val førsteArbeidsforholdInntektData =
    genererAinntektsdata(
        beloep = BigDecimal.valueOf(50000),
        fraMaaned = YearMonth.of(2025, 1),
        virksomhetsnummer = førsteArbeidsgiverOrgnummer,
        antallMaanederTilbake = 12,
    )

private val betongByggInntektsmelding = UUID.randomUUID().toString()

// Inntektsmelding for første arbeidsforhold
private val inntektsmeldinger =
    listOf(
        skapInntektsmelding(
            inntektsmeldingId = betongByggInntektsmelding,
            månedsinntekt = 21666.67,
            organisasjon = betongbyggAS,
            arbeidstakerFnr = fnr,
            refusjon = Refusjon(beloepPrMnd = BigDecimal("21666.67"), opphoersdato = null),
        ),
    )

// Søknads-IDer
private val førsteSøknadId = UUID.randomUUID()
private val førsteSøknadId2 = UUID.randomUUID()
private val andreSøknadId = UUID.randomUUID()

val forlengelseMedNyJobbOgRefusjon =
    Testscenario(
        tittel = "Forlengelse med ny jobb og refusjon",
        testperson =
            Testperson(
                fornavn = "Forleng",
                fødselsdato = LocalDate.now().minusYears(35),
                fnr = fnr,
                spilleromId = "gefddfg",
                etternavn = "Forlang",
                aaregData =
                    listOf(
                        // Første arbeidsforhold: Betongbygg AS fra 01.01.24 - fortsatt aktiv
                        arbeidsforhold(
                            fnr = fnr,
                            orgnummer = førsteArbeidsgiverOrgnummer,
                            startdato = LocalDate.of(2024, 1, 1),
                            sluttdato = null, // Fortsatt aktiv
                            stillingsprosent = 100.0,
                        ),
                        // Andre arbeidsforhold: Veihjelpen AS fra 01.06.25 - fortsatt aktiv
                        arbeidsforhold(
                            fnr = fnr,
                            orgnummer = andreArbeidsgiverOrgnummer,
                            startdato = LocalDate.of(2025, 6, 1),
                            sluttdato = null, // Fortsatt aktiv
                            stillingsprosent = 100.0,
                        ),
                    ),
                ainntektData = førsteArbeidsforholdInntektData,
                inntektsmeldinger = inntektsmeldinger,
                soknader =
                    listOf(
                        // Første søknad: Sykmeldt fra første virksomhet (Betongbygg AS)
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2025, 5, 1),
                            tom = LocalDate.of(2025, 5, 31),
                        ) {
                            id = førsteSøknadId
                            type = SoknadstypeDTO.ARBEIDSTAKERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidstaker(betongbyggAS)
                            grad = 100
                        },
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2025, 6, 1),
                            tom = LocalDate.of(2025, 6, 30),
                        ) {
                            id = førsteSøknadId2
                            type = SoknadstypeDTO.ARBEIDSTAKERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidstaker(betongbyggAS)
                            grad = 100
                        },
                        // Andre søknad: Direkte sykmeldt i ny virksomhet (Veihjelpen AS)
                        // Starter dagen etter første søknad slutter (forlengelse)
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2025, 6, 1),
                            tom = LocalDate.of(2025, 6, 30),
                        ) {
                            id = andreSøknadId
                            type = SoknadstypeDTO.ARBEIDSTAKERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidstaker(veihjelpenAS)
                            grad = 100
                        },
                    ),
                saksbehandingsperioder =
                    listOf(
                        Saksbehandingsperiode(
                            fom = LocalDate.of(2025, 5, 1),
                            tom = LocalDate.of(2025, 5, 31),
                            søknadIder = setOf(førsteSøknadId),
                            avsluttet = true,
                            inntektRequest =
                                InntektRequest.Arbeidstaker(
                                    ArbeidstakerInntektRequest.Inntektsmelding(
                                        inntektsmeldingId = betongByggInntektsmelding,
                                        begrunnelse = "sdfsdf",
                                        refusjon =
                                            listOf(
                                                Refusjonsperiode(
                                                    fom = LocalDate.of(2025, 5, 1),
                                                    tom = null,
                                                    beløp = InntektbeløpDto.MånedligDouble(21666.67),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                        Saksbehandingsperiode(
                            fom = LocalDate.of(2025, 6, 1),
                            tom = LocalDate.of(2025, 6, 30),
                            søknadIder = setOf(førsteSøknadId2, andreSøknadId),
                        ),
                    ),
            ),
        beskrivelse =
            """
            TODO beskriv
            """.trimIndent(),
    )
