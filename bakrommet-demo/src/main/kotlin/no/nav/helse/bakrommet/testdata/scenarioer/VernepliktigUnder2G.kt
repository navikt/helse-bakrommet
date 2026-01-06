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

private val kioskOrgnummer = "999888111"
private val maanedsinntekt = BigDecimal.valueOf(12500) // 150 000 kr per år

// Inntektsdata fra kiosken - jobbet i 2 år før verneplikten startet 1. august 2025
private val kioskInntektData =
    genererAinntektsdata(
        beloep = maanedsinntekt,
        fraMaaned = YearMonth.of(2025, 7), // Juli 2025 (siste måned før verneplikten)
        virksomhetsnummer = kioskOrgnummer,
        antallMaanederTilbake = 24, // 2 år tilbake
    )

val vernepliktigUnder2G =
    Testscenario(
        tittel = "Vernepliktig som tjente under 2G før tjenesten",
        testperson =
            Testperson(
                fornavn = "Knut",
                fødselsdato = LocalDate.now().minusYears(19),
                fnr = "19058812345",
                etternavn = "Andersen",
                aaregData =
                    genererAaregFraAinntekt(
                        fnr = "19058812345",
                        ainntektData = kioskInntektData,
                        fortsattAktiveOrgnummer = emptyList(), // Ikke lenger aktiv da han startet verneplikt
                    ),
                ainntektData = kioskInntektData,
                soknader =
                    listOf(
                        soknad(
                            fnr = "19058812345",
                            fom = LocalDate.of(2025, 10, 1),
                            tom = LocalDate.of(2025, 10, 31),
                        ) {
                            id = søknadsid
                            type = SoknadstypeDTO.ANNET_ARBEIDSFORHOLD
                            status = SoknadsstatusDTO.SENDT
                            grad = 100
                            sykmeldingSkrevet = LocalDate.of(2025, 10, 1)
                            startSyketilfelle = LocalDate.of(2025, 10, 1)
                            opprettet = LocalDate.of(2025, 10, 1)
                            sendtNav = LocalDate.of(2025, 10, 5)
                        },
                    ),
            ),
        beskrivelse =
            """
            Vernepliktig som jobbet på kiosk i 2 år før tjenesten startet.
            
            Situasjon før verneplikt: 
            - Jobbet på kiosk fra august 2023 til juli 2025
            - Tjente 12 500 kr per måned (150 000 kr per år)
            - Dette er under 2G (grunnbeløpet i 2025 er 130 160 kr, så 2G = 260 320 kr)
            
            Verneplikten:
            - Startet 1. august 2025
            - Ble sykmeldt under tjenesten i oktober 2025
            """.trimIndent(),
    )
