package no.nav.helse.bakrommet.testdata

import no.nav.helse.bakrommet.inntektsmelding.enInntektsmelding
import no.nav.helse.bakrommet.testdata.scenarioer.alleScenarioer
import no.nav.helse.flex.sykepengesoknad.kafka.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun alleTestdata(): List<Testperson> =
    mutableListOf(
        kalleKranfører(),
        mattisMatros(),
        bosseBunntrål(),
        blankeArk(),
        muggeMcMurstein(),
    ).also { lista -> lista.addAll(alleScenarioer.map { it.testperson }) }

private fun kalleKranfører() =
    Testperson(
        fnr = "12345678901",
        aktorId = "1234567891011",
        spilleromId = "8j4ns",
        fornavn = "Kalle",
        etternavn = "Kranfører",
        fødselsdato = LocalDate.of(1977, 1, 1), // ca. 47 år basert på alder 47
        inntektsmeldinger =
            listOf(
                enInntektsmelding(UUID.randomUUID().toString()),
                enInntektsmelding(UUID.randomUUID().toString(), beregnetInntekt = 89000.0),
            ),
        saksbehandingsperioder =
            listOf(
                Saksbehandingsperiode(
                    fom = LocalDate.of(2025, 9, 1),
                    tom = LocalDate.of(2025, 9, 20),
                    søknadIder = listOf("8"),
                ),
                Saksbehandingsperiode(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 2, 28),
                    søknadIder = listOf("3", "4", "5"),
                ),
                Saksbehandingsperiode(
                    fom = LocalDate.of(2024, 8, 2),
                    tom = LocalDate.of(2024, 8, 9),
                    søknadIder = listOf("1"),
                ),
            ),
        soknader =
            listOf(
                lagSøknad(
                    id = "1",
                    fnr = "12345678901",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    status = SoknadsstatusDTO.NY,
                    fom = LocalDate.of(2024, 8, 2),
                    tom = LocalDate.of(2024, 8, 9),
                    arbeidsgiverNavn = "Kranførerkompaniet",
                    arbeidsgiverOrgnummer = "987654321",
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    sykmeldingSkrevet = LocalDate.of(2024, 8, 2),
                    startSyketilfelle = LocalDate.of(2024, 8, 2),
                    opprettet = LocalDate.of(2024, 8, 2),
                    sendtNav = LocalDate.of(2024, 8, 2),
                    sendtArbeidsgiver = LocalDate.of(2024, 8, 2),
                ),
                lagSøknad(
                    id = "2",
                    fnr = "12345678901",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    status = SoknadsstatusDTO.NY,
                    fom = LocalDate.of(2024, 8, 10),
                    tom = LocalDate.of(2024, 9, 22),
                    arbeidsgiverNavn = "Kranførerkompaniet",
                    arbeidsgiverOrgnummer = "987654321",
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    sykmeldingSkrevet = LocalDate.of(2024, 8, 10),
                    startSyketilfelle = LocalDate.of(2024, 8, 10),
                    opprettet = LocalDate.of(2024, 8, 10),
                    sendtNav = LocalDate.of(2024, 8, 10),
                    sendtArbeidsgiver = LocalDate.of(2024, 8, 10),
                ),
                lagSøknad(
                    id = "3",
                    fnr = "12345678901",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    status = SoknadsstatusDTO.NY,
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                    arbeidsgiverNavn = "Kranførerkompaniet",
                    arbeidsgiverOrgnummer = "987654321",
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    sykmeldingSkrevet = LocalDate.of(2025, 1, 1),
                    startSyketilfelle = LocalDate.of(2025, 1, 1),
                    opprettet = LocalDate.of(2025, 1, 1),
                    sendtNav = LocalDate.of(2025, 1, 1),
                    sendtArbeidsgiver = LocalDate.of(2025, 1, 1),
                ),
                lagSøknad(
                    id = "4",
                    fnr = "12345678901",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    status = SoknadsstatusDTO.NY,
                    fom = LocalDate.of(2025, 2, 1),
                    tom = LocalDate.of(2025, 2, 28),
                    arbeidsgiverNavn = "Kranførerkompaniet",
                    arbeidsgiverOrgnummer = "987654321",
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    sykmeldingSkrevet = LocalDate.of(2025, 1, 1),
                    startSyketilfelle = LocalDate.of(2025, 1, 1),
                    opprettet = LocalDate.of(2025, 1, 1),
                    sendtNav = LocalDate.of(2025, 1, 1),
                    sendtArbeidsgiver = LocalDate.of(2025, 1, 1),
                ),
                lagSøknad(
                    id = "5",
                    fnr = "12345678901",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    status = SoknadsstatusDTO.NY,
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                    arbeidsgiverNavn = "Krankompisen",
                    arbeidsgiverOrgnummer = "123456789",
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    sykmeldingSkrevet = LocalDate.of(2025, 1, 1),
                    startSyketilfelle = LocalDate.of(2025, 1, 1),
                    opprettet = LocalDate.of(2025, 1, 1),
                    sendtNav = LocalDate.of(2025, 1, 1),
                    sendtArbeidsgiver = LocalDate.of(2025, 1, 1),
                ),
                lagSøknad(
                    id = "6",
                    fnr = "12345678901",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    status = SoknadsstatusDTO.NY,
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                    arbeidsgiverNavn = "Krankompisen",
                    arbeidsgiverOrgnummer = "123456789",
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    sykmeldingSkrevet = LocalDate.of(2024, 1, 1),
                    startSyketilfelle = LocalDate.of(2024, 1, 1),
                    opprettet = LocalDate.of(2024, 1, 1),
                    sendtNav = LocalDate.of(2024, 1, 1),
                    sendtArbeidsgiver = LocalDate.of(2024, 1, 1),
                ),
                lagSøknad(
                    id = "7",
                    fnr = "12345678901",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    status = SoknadsstatusDTO.NY,
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 31),
                    arbeidsgiverNavn = "Krankompisen",
                    arbeidsgiverOrgnummer = "123456789",
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    sykmeldingSkrevet = LocalDate.of(2023, 1, 1),
                    startSyketilfelle = LocalDate.of(2023, 1, 1),
                    opprettet = LocalDate.of(2023, 1, 1),
                    sendtNav = LocalDate.of(2023, 1, 1),
                    sendtArbeidsgiver = LocalDate.of(2023, 1, 1),
                ),
                lagSøknad(
                    id = "8",
                    fnr = "12345678901",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    status = SoknadsstatusDTO.NY,
                    fom = LocalDate.of(2025, 9, 1),
                    tom = LocalDate.of(2025, 9, 20),
                    arbeidsgiverNavn = "Krankompisen",
                    arbeidsgiverOrgnummer = "123456789",
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    sykmeldingSkrevet = LocalDate.of(2025, 9, 1),
                    startSyketilfelle = LocalDate.of(2025, 9, 1),
                    opprettet = LocalDate.of(2025, 9, 1),
                    sendtNav = LocalDate.of(2025, 9, 21),
                    sendtArbeidsgiver = LocalDate.of(2025, 9, 21),
                ),
                lagSøknad(
                    id = "9",
                    fnr = "12345678901",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    status = SoknadsstatusDTO.NY,
                    fom = LocalDate.of(2025, 9, 21),
                    tom = LocalDate.of(2025, 9, 30),
                    arbeidsgiverNavn = "Krankompisen",
                    arbeidsgiverOrgnummer = "123456789",
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    sykmeldingSkrevet = LocalDate.of(2025, 9, 21),
                    startSyketilfelle = LocalDate.of(2025, 9, 1),
                    opprettet = LocalDate.of(2025, 9, 21),
                    sendtNav = LocalDate.of(2025, 9, 21),
                    sendtArbeidsgiver = LocalDate.of(2025, 9, 21),
                ),
            ),
    )

private fun mattisMatros() =
    Testperson(
        fnr = "13065212348",
        aktorId = "1234567891011",
        spilleromId = "jf74h",
        fornavn = "Mattis",
        etternavn = "Matros",
        fødselsdato = LocalDate.of(2006, 6, 12), // ca. 18 år basert på alder 18
        saksbehandingsperioder = emptyList(),
        soknader =
            listOf(
                lagSøknad(
                    id = "ab0c5c03-0acf-3e42-a0cc-fa74281d5bba",
                    fnr = "13065212348",
                    type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
                    status = SoknadsstatusDTO.SENDT,
                    fom = LocalDate.of(2025, 6, 1),
                    tom = LocalDate.of(2025, 6, 24),
                    arbeidsgiverNavn = null,
                    arbeidsgiverOrgnummer = null,
                    arbeidssituasjon = ArbeidssituasjonDTO.FRILANSER,
                    sykmeldingSkrevet = LocalDate.of(2025, 6, 18),
                    startSyketilfelle = LocalDate.of(2025, 5, 27),
                    opprettet = LocalDate.of(2025, 6, 25),
                    sendtNav = LocalDate.of(2025, 6, 25),
                    sendtArbeidsgiver = null,
                    sykmeldingstype = SykmeldingstypeDTO.AKTIVITET_IKKE_MULIG,
                ),
            ),
    )

private fun bosseBunntrål() =
    Testperson(
        fnr = "30816199456",
        aktorId = "3081619945600",
        spilleromId = "bosse",
        fornavn = "Bosse",
        mellomnavn = "B",
        etternavn = "Bunntrål",
        fødselsdato = LocalDate.of(1963, 8, 16), // ca. 61 år basert på alder 61
        saksbehandingsperioder = emptyList(),
        soknader =
            listOf(
                lagSøknad(
                    id = "1b002e4f-1559-3415-a940-56eddf83e4d3",
                    fnr = "30816199456",
                    type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
                    status = SoknadsstatusDTO.SENDT,
                    fom = LocalDate.of(2025, 6, 9),
                    tom = LocalDate.of(2025, 6, 15),
                    arbeidsgiverNavn = null,
                    arbeidsgiverOrgnummer = null,
                    arbeidssituasjon = ArbeidssituasjonDTO.FISKER,
                    sykmeldingSkrevet = LocalDate.of(2025, 6, 9),
                    startSyketilfelle = LocalDate.of(2025, 6, 9),
                    opprettet = LocalDate.of(2025, 6, 16),
                    sendtNav = LocalDate.of(2025, 6, 16),
                    sendtArbeidsgiver = null,
                    sykmeldingstype = SykmeldingstypeDTO.AKTIVITET_IKKE_MULIG,
                ),
            ),
    )

private fun blankeArk() =
    Testperson(
        fnr = "13064512348",
        aktorId = "1234567891011",
        spilleromId = "blank",
        fornavn = "Blanke",
        etternavn = "Ark",
        fødselsdato = LocalDate.of(2003, 6, 13), // ca. 22 år basert på alder 22
        saksbehandingsperioder =
            listOf(
                Saksbehandingsperiode(
                    fom = LocalDate.of(2025, 3, 1),
                    tom = LocalDate.of(2025, 3, 31),
                ),
            ),
        soknader = emptyList(),
    )

private fun muggeMcMurstein() =
    Testperson(
        fnr = "01020304050",
        aktorId = "0102030405099",
        spilleromId = "mugge",
        fornavn = "Mugge",
        etternavn = "McMurstein",
        fødselsdato = LocalDate.of(1959, 1, 2), // ca. 65 år basert på alder 65
        saksbehandingsperioder = emptyList(),
        soknader = generateMuggeSoknader(),
    )

private fun generateMuggeSoknader(): List<SykepengesoknadDTO> {
    val arbeidsgiver1 = Pair("Murstein AS", "999999991")
    val arbeidsgiver2 = Pair("Betongbygg AS", "999999992")

    val start1 = LocalDate.of(2025, 1, 1)
    val start2 = LocalDate.of(2025, 1, 8)

    val soknader = mutableListOf<SykepengesoknadDTO>()
    var prevTom1 = start1
    var prevTom2 = start2

    for (i in 0 until 5) {
        // Arbeidsforhold 1
        val fom1 = if (i == 0) start1 else prevTom1.plusDays(1)
        val tom1 = fom1.plusDays(13)
        soknader.add(
            lagSøknad(
                id = "mugge-1-${i + 1}",
                fnr = "01020304050",
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.NY,
                fom = fom1,
                tom = tom1,
                arbeidsgiverNavn = arbeidsgiver1.first,
                arbeidsgiverOrgnummer = arbeidsgiver1.second,
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                sykmeldingSkrevet = fom1,
                startSyketilfelle = fom1,
                opprettet = fom1,
                sendtNav = fom1,
                sendtArbeidsgiver = fom1,
            ),
        )
        prevTom1 = tom1

        // Arbeidsforhold 2
        val fom2 = if (i == 0) start2 else prevTom2.plusDays(1)
        val tom2 = fom2.plusDays(13)
        soknader.add(
            lagSøknad(
                id = "mugge-2-${i + 1}",
                fnr = "01020304050",
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.NY,
                fom = fom2,
                tom = tom2,
                arbeidsgiverNavn = arbeidsgiver2.first,
                arbeidsgiverOrgnummer = arbeidsgiver2.second,
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                sykmeldingSkrevet = fom2,
                startSyketilfelle = fom2,
                opprettet = fom2,
                sendtNav = fom2,
                sendtArbeidsgiver = fom2,
            ),
        )
        prevTom2 = tom2
    }

    return soknader
}

private fun lagSøknad(
    id: String,
    fnr: String,
    type: SoknadstypeDTO,
    status: SoknadsstatusDTO,
    fom: LocalDate,
    tom: LocalDate,
    arbeidsgiverNavn: String? = null,
    arbeidsgiverOrgnummer: String? = null,
    arbeidssituasjon: ArbeidssituasjonDTO? = null,
    sykmeldingSkrevet: LocalDate? = null,
    startSyketilfelle: LocalDate? = null,
    opprettet: LocalDate? = null,
    sendtNav: LocalDate? = null,
    sendtArbeidsgiver: LocalDate? = null,
    sykmeldingstype: SykmeldingstypeDTO = SykmeldingstypeDTO.AKTIVITET_IKKE_MULIG,
): SykepengesoknadDTO =
    SykepengesoknadDTO(
        id = id,
        fnr = fnr,
        type = type,
        status = status,
        fom = fom,
        tom = tom,
        arbeidsgiver =
            arbeidsgiverNavn?.let { navn ->
                arbeidsgiverOrgnummer?.let { orgnummer ->
                    ArbeidsgiverDTO(
                        navn = navn,
                        orgnummer = orgnummer,
                    )
                }
            },
        arbeidssituasjon = arbeidssituasjon,
        sykmeldingSkrevet = sykmeldingSkrevet?.let { LocalDateTime.of(it, java.time.LocalTime.of(2, 0)) },
        startSyketilfelle = startSyketilfelle,
        arbeidGjenopptatt = null,
        opprettet = opprettet?.atStartOfDay(),
        sendtNav = sendtNav?.atStartOfDay(),
        sendtArbeidsgiver = sendtArbeidsgiver?.atStartOfDay(),
        soknadsperioder =
            listOf(
                SoknadsperiodeDTO(
                    fom = fom,
                    tom = tom,
                    grad = 100,
                    sykmeldingsgrad = 100,
                    faktiskGrad = null,
                    sykmeldingstype = sykmeldingstype,
                    avtaltTimer = null,
                    faktiskTimer = null,
                ),
            ),
        fravar = emptyList(),
        egenmeldinger = null,
        fravarForSykmeldingen = emptyList(),
        papirsykmeldinger = emptyList(),
        andreInntektskilder = emptyList(),
        sporsmal = emptyList(),
        korrigerer = null,
        korrigertAv = null,
        soktUtenlandsopphold = false,
        arbeidsgiverForskutterer = null,
        dodsdato = null,
        friskmeldt = null,
        opprinneligSendt = null,
    )
