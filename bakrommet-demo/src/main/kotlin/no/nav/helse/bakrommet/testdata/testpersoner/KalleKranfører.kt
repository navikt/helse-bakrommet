package no.nav.helse.bakrommet.testdata.testpersoner

import no.nav.helse.bakrommet.inntektsmelding.enInntektsmelding
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Saksbehandingsperiode
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import java.time.LocalDate
import java.util.UUID

val kalleKranfører =
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
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 2, 28),
                    søknadIder = listOf("3", "4", "5"),
                ),
                Saksbehandingsperiode(
                    fom = LocalDate.of(2024, 8, 2),
                    tom = LocalDate.of(2024, 8, 9),
                    søknadIder = listOf("1"),
                    avsluttet = true,
                ),
            ),
        soknader =
            listOf(
                soknad(
                    fnr = "12345678901",
                    fom = LocalDate.of(2024, 8, 2),
                    tom = LocalDate.of(2024, 8, 9),
                ) {
                    id = "1"
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                    sykmeldingSkrevet = LocalDate.of(2024, 8, 2)
                    startSyketilfelle = LocalDate.of(2024, 8, 2)
                    opprettet = LocalDate.of(2024, 8, 2)
                    sendtNav = LocalDate.of(2024, 8, 2)
                    sendtArbeidsgiver = LocalDate.of(2024, 8, 2)
                },
                soknad(
                    fnr = "12345678901",
                    fom = LocalDate.of(2024, 8, 10),
                    tom = LocalDate.of(2024, 9, 22),
                ) {
                    id = "2"
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                    sykmeldingSkrevet = LocalDate.of(2024, 8, 10)
                    startSyketilfelle = LocalDate.of(2024, 8, 10)
                    opprettet = LocalDate.of(2024, 8, 10)
                    sendtNav = LocalDate.of(2024, 8, 10)
                    sendtArbeidsgiver = LocalDate.of(2024, 8, 10)
                },
                soknad(
                    fnr = "12345678901",
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                ) {
                    id = "3"
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                    sykmeldingSkrevet = LocalDate.of(2025, 1, 1)
                    startSyketilfelle = LocalDate.of(2025, 1, 1)
                    opprettet = LocalDate.of(2025, 1, 1)
                    sendtNav = LocalDate.of(2025, 1, 1)
                    sendtArbeidsgiver = LocalDate.of(2025, 1, 1)
                },
                soknad(
                    fnr = "12345678901",
                    fom = LocalDate.of(2025, 2, 1),
                    tom = LocalDate.of(2025, 2, 28),
                ) {
                    id = "4"
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                    sykmeldingSkrevet = LocalDate.of(2025, 1, 1)
                    startSyketilfelle = LocalDate.of(2025, 1, 1)
                    opprettet = LocalDate.of(2025, 1, 1)
                    sendtNav = LocalDate.of(2025, 1, 1)
                    sendtArbeidsgiver = LocalDate.of(2025, 1, 1)
                },
                soknad(
                    fnr = "12345678901",
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                ) {
                    id = "5"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                    sykmeldingSkrevet = LocalDate.of(2025, 1, 1)
                    startSyketilfelle = LocalDate.of(2025, 1, 1)
                    opprettet = LocalDate.of(2025, 1, 1)
                    sendtNav = LocalDate.of(2025, 1, 1)
                    sendtArbeidsgiver = LocalDate.of(2025, 1, 1)
                },
                soknad(
                    fnr = "12345678901",
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                ) {
                    id = "6"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                    sykmeldingSkrevet = LocalDate.of(2024, 1, 1)
                    startSyketilfelle = LocalDate.of(2024, 1, 1)
                    opprettet = LocalDate.of(2024, 1, 1)
                    sendtNav = LocalDate.of(2024, 1, 1)
                    sendtArbeidsgiver = LocalDate.of(2024, 1, 1)
                },
                soknad(
                    fnr = "12345678901",
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 31),
                ) {
                    id = "7"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                    sykmeldingSkrevet = LocalDate.of(2023, 1, 1)
                    startSyketilfelle = LocalDate.of(2023, 1, 1)
                    opprettet = LocalDate.of(2023, 1, 1)
                    sendtNav = LocalDate.of(2023, 1, 1)
                    sendtArbeidsgiver = LocalDate.of(2023, 1, 1)
                },
                soknad(
                    fnr = "12345678901",
                    fom = LocalDate.of(2025, 9, 1),
                    tom = LocalDate.of(2025, 9, 20),
                ) {
                    id = "8"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                    sykmeldingSkrevet = LocalDate.of(2025, 9, 1)
                    startSyketilfelle = LocalDate.of(2025, 9, 1)
                    opprettet = LocalDate.of(2025, 9, 1)
                    sendtNav = LocalDate.of(2025, 9, 21)
                    sendtArbeidsgiver = LocalDate.of(2025, 9, 21)
                },
                soknad(
                    fnr = "12345678901",
                    fom = LocalDate.of(2025, 9, 21),
                    tom = LocalDate.of(2025, 9, 30),
                ) {
                    id = "9"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                    sykmeldingSkrevet = LocalDate.of(2025, 9, 21)
                    startSyketilfelle = LocalDate.of(2025, 9, 1)
                    opprettet = LocalDate.of(2025, 9, 21)
                    sendtNav = LocalDate.of(2025, 9, 21)
                    sendtArbeidsgiver = LocalDate.of(2025, 9, 21)
                },
            ),
    )
