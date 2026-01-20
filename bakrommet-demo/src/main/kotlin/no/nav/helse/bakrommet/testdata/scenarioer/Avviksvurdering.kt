package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
import no.nav.helse.bakrommet.ereg.malermesternAS
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

private val søknadsid = UUID.randomUUID()

private val inntektData =
    genererAinntektsdata(
        beloep = BigDecimal.valueOf(40000),
        fraMaaned = YearMonth.of(2025, 10),
        organisasjon = malermesternAS,
        antallMaanederTilbake = 16,
    )

private val fnr = "10029714444"
private val inntektsmeldinger =
    listOf(
        skapInntektsmelding(
            månedsinntekt = 89000.0,
            organisasjon = malermesternAS,
            arbeidstakerFnr = fnr,
            foersteFravaersdag = LocalDate.of(2025, 9, 29),
        ),
    )
val avvikendeMaler =
    Testscenario(
        tittel = "Har 25% avvik mot sammenlikningsgrunnlaget",
        testperson =
            Testperson(
                fornavn = "Avvikende",
                fødselsdato = LocalDate.now().minusYears(20),
                fnr = fnr,
                inntektsmeldinger = inntektsmeldinger,
                etternavn = "Maler",
                ainntektData = inntektData,
                soknader =
                    listOf(
                        soknad(
                            fnr = fnr,
                            fom = LocalDate.of(2025, 9, 29),
                            tom = LocalDate.of(2025, 10, 26),
                        ) {
                            arbeidstaker(malermesternAS)
                            id = søknadsid
                            status = SoknadsstatusDTO.SENDT
                            grad = 100
                        },
                    ),
            ),
        beskrivelse =
            """
            Inntektsmeldingen rapporterer 89.000 i beregnet inntekt.
            
            A-inntekt for sammenlikningsgrunnlaget er 40.000 per mnd.
            """.trimIndent(),
    )
