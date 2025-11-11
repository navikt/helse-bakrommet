package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.aareg.arbeidsforhold
import no.nav.helse.bakrommet.aareg.midlertidigAnsettelse
import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
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

private val fnr1 = "15069212345"
private val førsteArbeidsgiverOrgnummer = "211222333"

// Inntektsdata for første arbeidsforhold (deltid): 20000 kr/mnd
private val førsteArbeidsforholdInntektData =
    genererAinntektsdata(
        beloep = BigDecimal.valueOf(20000),
        fraMaaned = YearMonth.of(2025, 3),
        virksomhetsnummer = førsteArbeidsgiverOrgnummer,
        antallMaanederTilbake = 6,
    )

// Inntektsdata for andre arbeidsforhold (fulltid): 60000 kr/mnd
private val andreArbeidsforholdInntektData =
    genererAinntektsdata(
        beloep = BigDecimal.valueOf(60000),
        fraMaaned = YearMonth.of(2025, 4),
        organisasjon = veihjelpenAS,
        antallMaanederTilbake = 1,
    )

// Kombinert inntektsdata
private val kombinertInntektData = førsteArbeidsforholdInntektData + andreArbeidsforholdInntektData

// Inntektsmelding for andre arbeidsforhold
private val inntektsmeldinger =
    listOf(
        skapInntektsmelding(
            beregnetInntekt = 60000.0,
            organisasjon = veihjelpenAS,
            arbeidstakerFnr = fnr1,
        ),
    )

val tidsbegrensetArbeidsforhold =
    Testscenario(
        tittel = "Tidsbegrenset arbeidsforhold",
        testperson =
            Testperson(
                fornavn = "Viktor",
                fødselsdato = LocalDate.now().minusYears(32),
                fnr = fnr1,
                spilleromId = "viktor",
                etternavn = "Vikar",
                aaregData =
                    listOf(
                        // Første arbeidsforhold: vikar deltid fra 15.09.24 - 31.03.25
                        arbeidsforhold(
                            fnr = fnr1,
                            orgnummer = førsteArbeidsgiverOrgnummer,
                            startdato = LocalDate.of(2024, 9, 15),
                            sluttdato = LocalDate.of(2025, 3, 31),
                            stillingsprosent = 50.0, // Deltid
                            timerPrUke = 18.75,
                            ansettelsesform = midlertidigAnsettelse(),
                        ),
                        // Andre arbeidsforhold: vikariat fulltid fra 01.04.25 - 31.08.25
                        arbeidsforhold(
                            fnr = fnr1,
                            orgnummer = veihjelpenAS.first,
                            startdato = LocalDate.of(2025, 4, 1),
                            sluttdato = LocalDate.of(2025, 8, 31),
                            stillingsprosent = 100.0, // Fulltid
                            ansettelsesform = midlertidigAnsettelse(),
                        ),
                    ),
                ainntektData = kombinertInntektData,
                inntektsmeldinger = inntektsmeldinger,
                soknader =
                    listOf(
                        soknad(
                            fnr = fnr1,
                            fom = LocalDate.of(2025, 7, 7),
                            tom = LocalDate.of(2025, 8, 4),
                        ) {
                            id = UUID.randomUUID().toString()
                            type = SoknadstypeDTO.ARBEIDSTAKERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidstaker(veihjelpenAS)
                            grad = 100
                        },
                    ),
            ),
        beskrivelse =
            """
            Jobbet som vikar deltid fra 15.09.24 - 31.03.25.
            Fikk vikariat i full stilling hos annen arbeidsgiver fra 01.04.25 - 31.08.25.
            Ble syk underveis i dette arbeidsforholdet.
            """.trimIndent(),
    )
