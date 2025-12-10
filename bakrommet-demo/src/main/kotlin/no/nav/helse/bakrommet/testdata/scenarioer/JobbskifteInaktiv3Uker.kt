package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.aareg.arbeidsforhold
import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
import no.nav.helse.bakrommet.ereg.betongbyggAS
import no.nav.helse.bakrommet.ereg.klonelabben
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.sigrun.sigrunÅr
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.UUID

private val fnr = "15087512345"
private val gammelArbeidsgiverOrgnummer = betongbyggAS.orgnummer
private val nyArbeidsgiverOrgnummer = klonelabben.orgnummer

// Månedsinntekt på 62500 kroner tilsvarer årslønn på 750 000 kroner
private val maanedsinntekt = BigDecimal.valueOf(62500)

// Sigrun data som viser konstant årslønn på 750 000 kroner de siste årene
private val sigrunData =
    mapOf(
        Year.of(2022) to sigrunÅr(fnr = fnr, år = Year.of(2022), lønnsinntekt = 750000),
        Year.of(2023) to sigrunÅr(fnr = fnr, år = Year.of(2023), lønnsinntekt = 750000),
        Year.of(2024) to sigrunÅr(fnr = fnr, år = Year.of(2024), lønnsinntekt = 750000),
    )

// Inntektsdata for gammel arbeidsgiver (fram til 31.08.2025)
private val gammelArbeidsgiverInntektData =
    genererAinntektsdata(
        beloep = maanedsinntekt,
        fraMaaned = YearMonth.of(2025, 8),
        virksomhetsnummer = gammelArbeidsgiverOrgnummer,
        antallMaanederTilbake = 8, // Jan-Aug 2025
    )

// Inntektsdata for ny arbeidsgiver (fra 10.09.2025)
private val nyArbeidsgiverInntektData =
    genererAinntektsdata(
        beloep = maanedsinntekt,
        fraMaaned = YearMonth.of(2025, 11),
        organisasjon = klonelabben,
        antallMaanederTilbake = 3, // Sep-Nov 2025
    )

// Kombinert inntektsdata
private val kombinertInntektData = gammelArbeidsgiverInntektData + nyArbeidsgiverInntektData

// Inntektsmelding for ny arbeidsgiver
private val inntektsmeldinger =
    listOf(
        skapInntektsmelding(
            månedsinntekt = 62500.0, // Årsinntekt (62500 * 12)
            organisasjon = klonelabben,
            arbeidstakerFnr = fnr,
            foersteFravaersdag = LocalDate.of(2025, 9, 30),
        ) {
            medBegrunnelseForReduksjonEllerIkkeUtbetalt("Ikke utbetalt sykepenger i arbeidsgiverperioden pga manglende opptjening")
        },
    )

val jobbskifteInaktiv3uker =
    Testscenario(
        tittel = "Jobbskifte med 3 uker opphold periode",
        testperson =
            Testperson(
                fornavn = "Liv",
                fødselsdato = LocalDate.now().minusYears(42),
                fnr = fnr,
                etternavn = "Lindgren",
                aaregData =
                    listOf(
                        // Gammel jobb: mange år, siste dag 31.08.2025
                        arbeidsforhold(
                            fnr = fnr,
                            orgnummer = gammelArbeidsgiverOrgnummer,
                            startdato = LocalDate.of(2020, 1, 1), // Jobbet i mange år
                            sluttdato = LocalDate.of(2025, 8, 31), // Siste dag i gammel jobb
                            stillingsprosent = 100.0,
                        ),
                        // Ny jobb: starter 10.09.2025
                        arbeidsforhold(
                            fnr = fnr,
                            orgnummer = nyArbeidsgiverOrgnummer,
                            startdato = LocalDate.of(2025, 9, 10), // Starter i ny jobb
                            stillingsprosent = 100.0,
                            // Ingen sluttdato = fortsatt aktiv
                        ),
                    ),
                ainntektData = kombinertInntektData,
                sigrunData = sigrunData,
                inntektsmeldinger = inntektsmeldinger,
                soknader =
                    listOf(
                        // Blir sykmeldt etter 3 uker i ny jobb
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2025, 9, 30),
                            tom = LocalDate.of(2025, 10, 30),
                        ) {
                            id = UUID.randomUUID()
                            type = SoknadstypeDTO.ARBEIDSTAKERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidstaker(klonelabben)
                            grad = 100
                        },
                    ),
            ),
        beskrivelse =
            """
            Personen har jobbet i samme jobb i mange år. Har de siste årene hatt en årslønn på 750 000 kroner.
            Får tilbud om ny jobb i et annet firma og takker ja til dette.
            Har siste dag i gammel jobb 31.08.2025.
            Vil slappe av litt før hen begynner i den nye jobben, så starter der 10.09.2025.
            Etter å ha jobbet i ny jobb i tre uker blir personen sykmeldt.
            Arbeidsgiver sender IM hvor de oppgir at de ikke har utbetalt sykepenger i arbeidsgiverperioden pga manglende opptjening.
            Lønnen er oppgitt å være 62 500 kroner per måned.
            """.trimIndent(),
    )
