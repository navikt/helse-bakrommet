package no.nav.helse.bakrommet.testdata.scenarioer

import no.nav.helse.bakrommet.ainntekt.genererAinntektsdata
import no.nav.helse.bakrommet.ereg.betongbyggAS
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SporsmalDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SvarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SvartypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.VisningskriteriumDTO
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

private val søknadsid = UUID.randomUUID()

private val inntektData =
    genererAinntektsdata(
        beloep = BigDecimal.valueOf(14000),
        fraMaaned = YearMonth.of(2025, 10),
        organisasjon = betongbyggAS,
        antallMaanederTilbake = 16,
    )
private val søknadFom = LocalDate.of(2025, 9, 29)

private val fnr = "20029712322"

private val inntektsmeldinger =
    listOf(
        skapInntektsmelding(
            månedsinntekt = 14000.0,
            organisasjon = betongbyggAS,
            arbeidstakerFnr = fnr,
            mottattDato = LocalDateTime.of(2025, 5, 5, 11, 50, 0),
            foersteFravaersdag = søknadFom,
            refusjon = Refusjon(beloepPrMnd = BigDecimal("14000.00"), opphoersdato = null),
            arbeidsgiverperioder = listOf(Periode(søknadFom, søknadFom.plusDays(14))),
        ),
    )

val yrkesskade =
    Testscenario(
        tittel = "Sykmeldt pga yrkesskade som er 3 år gammel",
        testperson =
            Testperson(
                fornavn = "Uheldig",
                fødselsdato = LocalDate.now().minusYears(20),
                fnr = fnr,
                etternavn = "Arbeidskar",
                ainntektData = inntektData,
                inntektsmeldinger = inntektsmeldinger,
                soknader =
                    listOf(
                        soknad(
                            fnr = fnr,
                            fom = søknadFom,
                            tom = LocalDate.of(2025, 10, 26),
                        ) {
                            arbeidstaker(betongbyggAS)
                            id = søknadsid
                            status = SoknadsstatusDTO.SENDT
                            sporsmal =
                                listOf(
                                    SporsmalDTO(
                                        id = "8b567169-5a03-3f53-ad0c-622a5721821a",
                                        tag = "YRKESSKADE_V2",
                                        sporsmalstekst = "Skyldes dette sykefraværet en yrkesskade?",
                                        undertekst = null,
                                        min = null,
                                        max = null,
                                        svartype = SvartypeDTO.JA_NEI,
                                        kriterieForVisningAvUndersporsmal = VisningskriteriumDTO.JA,
                                        svar = listOf(SvarDTO(verdi = "JA")),
                                        undersporsmal =
                                            listOf(
                                                SporsmalDTO(
                                                    id = "4ff43cc5-6552-3a18-960a-46dc1e7f67f6",
                                                    tag = "YRKESSKADE_V2_VELG_DATO",
                                                    sporsmalstekst = "Hvilken skadedato skyldes dette sykefraværet? Du kan velge flere",
                                                    undertekst = null,
                                                    min = null,
                                                    max = null,
                                                    svartype = SvartypeDTO.CHECKBOX_GRUPPE,
                                                    kriterieForVisningAvUndersporsmal = null,
                                                    svar = emptyList(),
                                                    undersporsmal =
                                                        listOf(
                                                            SporsmalDTO(
                                                                id = "431441de-7942-3bf2-b852-b58cbd07dff9",
                                                                tag = "YRKESSKADE_V2_DATO",
                                                                sporsmalstekst = "Skadedato 2. januar 1982 (Vedtaksdato 2. januar 1989)",
                                                                undertekst = "1982-01-02",
                                                                min = null,
                                                                max = null,
                                                                svartype = SvartypeDTO.CHECKBOX,
                                                                kriterieForVisningAvUndersporsmal = null,
                                                                svar = emptyList(),
                                                                undersporsmal = emptyList(),
                                                                metadata = null,
                                                            ),
                                                            SporsmalDTO(
                                                                id = "03e62102-421f-31e9-aadc-9a0baf879512",
                                                                tag = "YRKESSKADE_V2_DATO",
                                                                sporsmalstekst = "Vedtaksdato 9. mai 1987",
                                                                undertekst = "1987-05-09",
                                                                min = null,
                                                                max = null,
                                                                svartype = SvartypeDTO.CHECKBOX,
                                                                kriterieForVisningAvUndersporsmal = null,
                                                                svar = emptyList(),
                                                                undersporsmal = emptyList(),
                                                                metadata = null,
                                                            ),
                                                            SporsmalDTO(
                                                                id = "753c1d45-9c47-3cbd-a958-babf7f068224",
                                                                tag = "YRKESSKADE_V2_DATO",
                                                                sporsmalstekst = "Skadedato 2. januar 2023 (Vedtaksdato 2. januar 2023)",
                                                                undertekst = "2023-01-02",
                                                                min = null,
                                                                max = null,
                                                                svartype = SvartypeDTO.CHECKBOX,
                                                                kriterieForVisningAvUndersporsmal = null,
                                                                svar = listOf(SvarDTO(verdi = "CHECKED")),
                                                                undersporsmal = emptyList(),
                                                                metadata = null,
                                                            ),
                                                            SporsmalDTO(
                                                                id = "b73f9c25-c8de-35c3-a9bf-c08fffbf7de3",
                                                                tag = "YRKESSKADE_V2_DATO",
                                                                sporsmalstekst = "Nylig registrert skade",
                                                                undertekst = null,
                                                                min = null,
                                                                max = null,
                                                                svartype = SvartypeDTO.CHECKBOX,
                                                                kriterieForVisningAvUndersporsmal = null,
                                                                svar = listOf(SvarDTO(verdi = "CHECKED")),
                                                                undersporsmal = emptyList(),
                                                                metadata = null,
                                                            ),
                                                        ),
                                                    metadata = null,
                                                ),
                                            ),
                                        metadata = null,
                                    ),
                                )
                            grad = 100
                            sykmeldingSkrevet = søknadFom
                            startSyketilfelle = søknadFom
                            opprettet = søknadFom
                            sendtNav = LocalDate.of(2025, 10, 1)
                        },
                    ),
            ),
        beskrivelse =
            """
            Opplyser i søknad at han er sykmeldt pga yrkesskade. 

            Tjente vesentlig mer på yrkessakde tidspunktet enn nåværende inntekt.
            """.trimIndent(),
    )
