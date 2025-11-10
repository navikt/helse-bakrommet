package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
import no.nav.helse.bakrommet.ereg.veihjelpenAS
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.sigrun.sigrunÅr
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.bakrommet.testdata.genererAaregFraAinntekt
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.UUID

private val fnr = "15078712345"
private val arbeidstakerSøknadIdNov = UUID.randomUUID().toString()
private val selvstendigSøknadIdDes = UUID.randomUUID().toString()
private val arbeidstakerSøknadIdJan = UUID.randomUUID().toString()
private val selvstendigSøknadIdJan = UUID.randomUUID().toString()

private val arbeidstakerInntektData =
    genererAinntektsdata(
        beloep = BigDecimal.valueOf(70000),
        fraMaaned = YearMonth.of(2025, 10),
        organisasjon = veihjelpenAS,
        antallMaanederTilbake = 50,
    )

private val sigrunData =
    mapOf(
        Year.of(2022) to sigrunÅr(fnr = fnr, år = Year.of(2022), næring = 400000, lønnsinntekt = 70000 * 12),
        Year.of(2023) to sigrunÅr(fnr = fnr, år = Year.of(2023), næring = 400000, lønnsinntekt = 70000 * 12),
        Year.of(2024) to sigrunÅr(fnr = fnr, år = Year.of(2024), næring = 400000, lønnsinntekt = 70000 * 12),
    )

private val inntektsmeldinger =
    listOf(
        skapInntektsmelding(beregnetInntekt = 70000.0, organisasjon = veihjelpenAS, arbeidstakerFnr = fnr),
    )

val kombinertArbeidstakerOgSelvstendig =
    Testscenario(
        tittel = "Kombinert arbeidstaker og selvstendig næringsdrivende",
        testperson =
            Testperson(
                fornavn = "Dobbel",
                fødselsdato = LocalDate.now().minusYears(37),
                fnr = fnr,
                spilleromId = "kombinert-arb-selv",
                etternavn = "Gundersen",
                aaregData =
                    genererAaregFraAinntekt(
                        fnr = fnr,
                        ainntektData = arbeidstakerInntektData,
                        fortsattAktiveOrgnummer = listOf(veihjelpenAS.first),
                    ),
                ainntektData = arbeidstakerInntektData,
                sigrunData = sigrunData,
                inntektsmeldinger = inntektsmeldinger,
                soknader =
                    listOf(
                        // November 2025: Kun sykmeldt som arbeidstaker, 100%
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2025, 11, 1),
                            tom = LocalDate.of(2025, 11, 30),
                        ) {
                            id = arbeidstakerSøknadIdNov
                            type = SoknadstypeDTO.ARBEIDSTAKERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidstaker(veihjelpenAS)
                            grad = 100
                        },
                        // Desember 2025: Kun sykmeldt som næringsdrivende, 100%
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2025, 12, 1),
                            tom = LocalDate.of(2025, 12, 31),
                        ) {
                            id = selvstendigSøknadIdDes
                            type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE
                            grad = 100
                        },
                        // Januar 2026: 50% syk som arbeidstaker
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2026, 1, 1),
                            tom = LocalDate.of(2026, 1, 31),
                        ) {
                            id = arbeidstakerSøknadIdJan
                            type = SoknadstypeDTO.ARBEIDSTAKERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidstaker(veihjelpenAS)
                            grad = 50
                        },
                        // Januar 2026: 50% syk som næringsdrivende
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2026, 1, 1),
                            tom = LocalDate.of(2026, 1, 31),
                        ) {
                            id = selvstendigSøknadIdJan
                            type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE
                            status = SoknadsstatusDTO.SENDT
                            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE
                            grad = 50
                        },
                    ),
            ),
        beskrivelse =
            """
            Kombinert arbeidstaker og selvstendig næringsdrivende.
            
            Arbeidstakerinntekt: 70 000 kr/mnd fra Veihjelpen AS
            Næringsinntekt: 400 000 kr/år (jevne inntekter siste tre ferdig lignede år)
            
            Syk fra 01.11.25 til 31.01.2026.
            
            Søknader:
            - November 2025: Kun sykmeldt som arbeidstaker, 100%
            - Desember 2025: Kun sykmeldt som næringsdrivende, 100%
            - Januar 2026: 50% syk i begge (to søknader, begge med 50% gradering)
            """.trimIndent(),
    )
