package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.aareg.arbeidsforhold
import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
import no.nav.helse.bakrommet.ereg.betongbyggAS
import no.nav.helse.bakrommet.ereg.klonelabben
import no.nav.helse.bakrommet.sigrun.sigrunÅr
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.UUID

private val fnr = "15087512346"
private val gammelArbeidsgiverOrgnummer = betongbyggAS.orgnummer
private val nyArbeidsgiverOrgnummer = klonelabben.orgnummer

// Månedsinntekt på 41667 kroner tilsvarer årslønn på 500 000 kroner
private val gammelMaanedsinntekt = BigDecimal.valueOf(41667)

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
        beloep = gammelMaanedsinntekt,
        fraMaaned = YearMonth.of(2025, 8),
        virksomhetsnummer = gammelArbeidsgiverOrgnummer,
        antallMaanederTilbake = 8, // Jan-Aug 2025
    )

val jobbskifteInaktivSykmeldt =
    Testscenario(
        tittel = "Jobbskifte inaktiv sykmeldt",
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
                        // Ny jobb: starter 01.10.2025 (etter sykmelding)
                        arbeidsforhold(
                            fnr = fnr,
                            orgnummer = nyArbeidsgiverOrgnummer,
                            startdato = LocalDate.of(2025, 10, 1), // Starter i ny jobb
                            stillingsprosent = 100.0,
                            // Ingen sluttdato = fortsatt aktiv
                        ),
                    ),
                ainntektData = gammelArbeidsgiverInntektData,
                sigrunData = sigrunData,
                inntektsmeldinger = emptyList(), // Ingen inntektsmelding siden personen ikke er i jobb på sykmeldingstidspunktet
                soknader =
                    listOf(
                        // Blir sykmeldt 20.09.2025 (før ny jobb starter)
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2025, 9, 20),
                            tom = LocalDate.of(2025, 10, 20),
                        ) {
                            id = UUID.randomUUID()
                            type = SoknadstypeDTO.ARBEIDSLEDIG
                            status = SoknadsstatusDTO.SENDT
                            arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSLEDIG // Ikke i jobb på sykmeldingstidspunktet
                            grad = 100
                        },
                    ),
            ),
        beskrivelse =
            """
            Personen har jobbet i samme jobb i mange år. Har de siste årene hatt en årslønn på 500 000 kroner.
            Er lei av jobben og vil lete etter ny jobb, og slutter derfor i jobben.
            Har siste dag i gammel jobb 31.08.2025.
            Den 10.09.2025 får personen tilbud om ny jobb med årslønn på 800 000 kroner.
            Det avtales at hen skal starte i den nye jobben 01.10.2025.
            Blir sykmeldt 20.09.2025.
            """.trimIndent(),
    )
