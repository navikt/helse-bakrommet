package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.bakrommet.testdata.genererAaregFraAinntekt
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

private val søknadsid = UUID.randomUUID()

private val skogenSFOOrgnummer = "999999993"
private val skogenSFOInntekt = BigDecimal.valueOf(28300)

private val skogenSFOAinntektData =
    genererAinntektsdata(
        beloep = skogenSFOInntekt,
        fraMaaned = YearMonth.of(2025, 6),
        virksomhetsnummer = skogenSFOOrgnummer,
        antallMaanederTilbake = 16,
    )

val vernepliktig =
    Testscenario(
        tittel = "Vernepliktig som dimitteres pga skade",
        testperson =
            Testperson(
                fornavn = "August",
                fødselsdato = LocalDate.now().minusYears(20),
                fnr = "20029712345",
                spilleromId = "91sto",
                etternavn = "Stomperud",
                aaregData =
                    genererAaregFraAinntekt(
                        fnr = "20029712345",
                        ainntektData = skogenSFOAinntektData,
                        fortsattAktiveOrgnummer = emptyList(), // Ikke lenger aktiv da han startet verneplikt
                    ),
                ainntektData = skogenSFOAinntektData,
                soknader =
                    listOf(
                        soknad(
                            fnr = "20029712345",
                            fom = LocalDate.of(2025, 9, 29),
                            tom = LocalDate.of(2025, 10, 26),
                        ) {
                            id = søknadsid
                            type = SoknadstypeDTO.ANNET_ARBEIDSFORHOLD
                            status = SoknadsstatusDTO.SENDT
                            grad = 100
                            sykmeldingSkrevet = LocalDate.of(2025, 9, 29)
                            startSyketilfelle = LocalDate.of(2025, 9, 29)
                            opprettet = LocalDate.of(2025, 9, 29)
                            sendtNav = LocalDate.of(2025, 10, 1)
                        },
                    ),
            ),
        beskrivelse =
            """
            Dimitert pga kneskade etter ca 3 mnd tjeneste.

            Situasjon før verneplikt: Jobbet som assistent ved SFO. 
            """.trimIndent(),
    )
