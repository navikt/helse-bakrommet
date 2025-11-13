package no.nav.helse.bakrommet.testdata.testpersoner

import no.nav.helse.bakrommet.ereg.kranførerkompaniet
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Saksbehandingsperiode
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.inntektsmeldingkontrakt.EndringIRefusjon
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

private val kalleFnr = "12345678901"
val kalleKranfører =
    Testperson(
        fnr = kalleFnr,
        aktorId = "1234567891011",
        spilleromId = "8j4ns",
        fornavn = "Kalle",
        etternavn = "Kranfører",
        fødselsdato = LocalDate.of(1977, 1, 1), // ca. 47 år basert på alder 47
        inntektsmeldinger =
            listOf(
                skapInntektsmelding(
                    inntektsmeldingId = UUID.randomUUID().toString(),
                    organisasjon = kranførerkompaniet,
                    arbeidstakerFnr = kalleFnr,
                    foersteFravaersdag = LocalDate.of(2025, 1, 1),
                    refusjon = Refusjon(beloepPrMnd = BigDecimal("50000.00"), opphoersdato = null),
                    endringIRefusjoner =
                        listOf(
                            EndringIRefusjon(
                                endringsdato = LocalDate.of(2025, 2, 1),
                                beloep = BigDecimal("45000.00"),
                            ),
                        ),
                    arbeidsgiverperioder = listOf(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 16))),
                    månedsinntekt = 50000.0,
                ),
                skapInntektsmelding(
                    UUID.randomUUID().toString(),
                    månedsinntekt = 89000.0,
                    arbeidstakerFnr = kalleFnr,
                ),
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
                    inntektRequest =
                        InntektRequest.Arbeidstaker(
                            ArbeidstakerInntektRequest.Skjønnsfastsatt(
                                InntektbeløpDto.Årlig(500000.0),
                                årsak = ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING,
                                begrunnelse = "Fordi",
                            ),
                        ),
                ),
            ),
        soknader =
            listOf(
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2024, 8, 2),
                    tom = LocalDate.of(2024, 8, 9),
                ) {
                    id = "1"
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2024, 8, 10),
                    tom = LocalDate.of(2024, 9, 22),
                ) {
                    id = "2"
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                ) {
                    id = "3"
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 2, 1),
                    tom = LocalDate.of(2025, 2, 28),
                ) {
                    id = "4"
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                ) {
                    id = "5"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                ) {
                    id = "6"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 31),
                ) {
                    id = "7"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 9, 1),
                    tom = LocalDate.of(2025, 9, 20),
                ) {
                    id = "8"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 9, 21),
                    tom = LocalDate.of(2025, 9, 30),
                ) {
                    id = "9"
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
            ),
    )
