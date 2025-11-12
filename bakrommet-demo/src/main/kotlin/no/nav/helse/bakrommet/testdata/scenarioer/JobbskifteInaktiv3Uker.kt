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
private val gammelArbeidsgiverOrgnummer = betongbyggAS.first
private val nyArbeidsgiverOrgnummer = klonelabben.first

// Månedsinntekt på 41667 kroner tilsvarer årslønn på 500 000 kroner
private val maanedsinntekt = BigDecimal.valueOf(41667)

// Sigrun data som viser konstant årslønn på 500 000 kroner de siste årene
private val sigrunData =
    mapOf(
        Year.of(2022) to sigrunÅr(fnr = fnr, år = Year.of(2022), lønnsinntekt = 500000),
        Year.of(2023) to sigrunÅr(fnr = fnr, år = Year.of(2023), lønnsinntekt = 500000),
        Year.of(2024) to sigrunÅr(fnr = fnr, år = Year.of(2024), lønnsinntekt = 500000),
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
            beregnetInntekt = 500000.0, // Årsinntekt
            organisasjon = klonelabben,
            arbeidstakerFnr = fnr,
        ),
    )

val jobbskifteInaktiv3uker =
    Testscenario(
        tittel = "Jobbskifte med kort autonom periode",
        testperson =
            Testperson(
                fornavn = "Liv",
                fødselsdato = LocalDate.now().minusYears(42),
                fnr = fnr,
                spilleromId = "liv-jobbskifte",
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
                        // Blir sykmeldt etter 3 uker i ny jobb (starter ca. 1. oktober)
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2025, 10, 1),
                            tom = LocalDate.of(2025, 10, 28),
                        ) {
                            id = UUID.randomUUID().toString()
                            type = SoknadstypeDTO.ARBEIDSTAKERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidstaker(klonelabben)
                            grad = 100
                        },
                    ),
            ),
        beskrivelse =
            """
            Personen har jobbet i samme jobb i mange år med konstant årslønn på 500 000 kroner.
            Siste dag i gammel jobb var 31.08.2025.
            Etter en kort autonom periode starter personen i ny jobb 10.09.2025.
            Etter å ha jobbet i ny jobb i tre uker blir personen 100% sykmeldt.
            """.trimIndent(),
    )
