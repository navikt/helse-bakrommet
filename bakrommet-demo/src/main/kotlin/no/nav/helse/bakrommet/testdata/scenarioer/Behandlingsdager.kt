package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
import no.nav.helse.bakrommet.ereg.plankeFabrikken
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykmeldingstypeDTO
import no.nav.helse.oktober
import no.nav.helse.september
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

private val søknadsid = UUID.randomUUID()

private val inntektData =
    genererAinntektsdata(
        beloep = BigDecimal.valueOf(40000),
        fraMaaned = YearMonth.of(2025, 10),
        organisasjon = plankeFabrikken,
        antallMaanederTilbake = 16,
    )

private val fnr = "10029714244"
private val fom = 29.september(2025)
private val inntektsmeldinger =
    listOf(
        skapInntektsmelding(
            månedsinntekt = 41000.0,
            organisasjon = plankeFabrikken,
            arbeidstakerFnr = fnr,
            mottattDato = fom.atTime(13, 37),
            foersteFravaersdag = fom,
            refusjon = Refusjon(beloepPrMnd = BigDecimal("41000.00"), opphoersdato = null),
            arbeidsgiverperioder =
                listOf(
                    Periode(fom, fom.plusDays(14)),
                ),
        ),
    )
val behandlingsdager =
    Testscenario(
        tittel = "Søknad om behandlingsdager",
        testperson =
            Testperson(
                fornavn = "Behandlings",
                fødselsdato = LocalDate.now().minusYears(30),
                fnr = fnr,
                inntektsmeldinger = inntektsmeldinger,
                spilleromId = "behandling",
                etternavn = "Dag",
                ainntektData = inntektData,
                soknader =
                    listOf(
                        soknad(
                            fnr = fnr,
                            fom = fom,
                            tom = LocalDate.of(2025, 10, 26),
                        ) {
                            arbeidstaker(plankeFabrikken)
                            id = søknadsid
                            status = SoknadsstatusDTO.SENDT
                            type = SoknadstypeDTO.BEHANDLINGSDAGER
                            sykmeldingstype = SykmeldingstypeDTO.BEHANDLINGSDAGER
                            grad = 100
                            valgteBehandlingsdager = listOf(29.september(2025), 8.oktober(2025))
                        },
                    ),
            ),
        beskrivelse =
            """
            Har søknad om behandlingsdager
            """.trimIndent(),
    )
