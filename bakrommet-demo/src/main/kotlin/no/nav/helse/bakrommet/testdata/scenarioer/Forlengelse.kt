package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.aareg.arbeidsforhold
import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
import no.nav.helse.bakrommet.ereg.betongbyggAS
import no.nav.helse.bakrommet.ereg.veihjelpenAS
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

private val fnr = "15089312345"
private val førsteArbeidsgiverOrgnummer = betongbyggAS.first
private val andreArbeidsgiverOrgnummer = veihjelpenAS.first

// Inntektsdata for første arbeidsforhold: 50000 kr/mnd
private val førsteArbeidsforholdInntektData =
    genererAinntektsdata(
        beloep = BigDecimal.valueOf(50000),
        fraMaaned = YearMonth.of(2025, 1),
        virksomhetsnummer = førsteArbeidsgiverOrgnummer,
        antallMaanederTilbake = 12,
    )

// Inntektsmelding for første arbeidsforhold
private val inntektsmeldinger =
    listOf(
        skapInntektsmelding(
            beregnetInntekt = 50000.0,
            organisasjon = betongbyggAS,
            arbeidstakerFnr = fnr,
        ),
    )

// Søknads-IDer
private val førsteSøknadId = UUID.randomUUID().toString()
private val andreSøknadId = UUID.randomUUID().toString()

val forlengelse =
    Testscenario(
        tittel = "Forlengelse med overtagelse av sykepengegrunnlag",
        testperson =
            Testperson(
                fornavn = "Forleng",
                fødselsdato = LocalDate.now().minusYears(35),
                fnr = fnr,
                spilleromId = "forlengelse",
                etternavn = "Grunnlag",
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
            ),
        beskrivelse =
            """
            Jobber i Betongbygg AS og er sykmeldt derfra fra 01.05.25 til 31.05.25.
            Deretter direkte sykmeldt i ny virksomhet Veihjelpen AS fra 01.06.25 til 30.06.25.
            
            Første virksomhet har inntektsmelding og data i ainntekt.
            Begge arbeidsforhold er registrert i aareg.
            
            Formålet ved testdata er å teste forlengelse med overtagelse av sykepengegrunnlag.
            """.trimIndent(),
    )
