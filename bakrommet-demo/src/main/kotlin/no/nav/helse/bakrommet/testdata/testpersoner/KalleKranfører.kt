package no.nav.helse.bakrommet.testdata.testpersoner

import no.nav.helse.bakrommet.behandling.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.ArbeidstakerSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.ereg.kranførerkompaniet
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.bakrommet.testdata.Behandling
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.inntektsmeldingkontrakt.EndringIRefusjon
import no.nav.inntektsmeldingkontrakt.GjenopptakelseNaturalytelse
import no.nav.inntektsmeldingkontrakt.InntektEndringAarsak
import no.nav.inntektsmeldingkontrakt.Naturalytelse
import no.nav.inntektsmeldingkontrakt.OpphoerAvNaturalytelse
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

private val kalleFnr = "12345678901"
val SOKNAD_ID_1 = UUID.randomUUID()
val SOKNAD_ID_2 = UUID.randomUUID()
val SOKNAD_ID_3 = UUID.randomUUID()
val SOKNAD_ID_4 = UUID.randomUUID()
val SOKNAD_ID_5 = UUID.randomUUID()
val SOKNAD_ID_6 = UUID.randomUUID()
val SOKNAD_ID_7 = UUID.randomUUID()
val SOKNAD_ID_8 = UUID.randomUUID()
val SOKNAD_ID_9 = UUID.randomUUID()

val kalleKranfører =
    Testperson(
        fnr = kalleFnr,
        fornavn = "Kalle",
        etternavn = "Kranfører",
        fødselsdato = LocalDate.of(1977, 1, 1), // ca. 47 år basert på alder 47
        inntektsmeldinger =
            listOf(
                skapInntektsmelding(
                    mottattDato = LocalDateTime.of(2025, 5, 5, 11, 50, 0),
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
                            EndringIRefusjon(
                                endringsdato = LocalDate.of(2025, 2, 4),
                                beloep = BigDecimal("46000.00"),
                            ),
                        ),
                    arbeidsgiverperioder = listOf(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 16))),
                    ferieperioder = listOf(Periode(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 16))),
                    månedsinntekt = 50000.0,
                    inntektEndringÅrsaker =
                        listOf(
                            InntektEndringAarsak(
                                aarsak = "NyStilling",
                                listOf(
                                    Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 16)),
                                    Periode(LocalDate.of(2025, 1, 17), LocalDate.of(2025, 1, 19)),
                                ),
                                gjelderFra = LocalDate.of(2025, 1, 17),
                                bleKjent = LocalDate.of(2025, 1, 17),
                            ),
                            InntektEndringAarsak(
                                aarsak = "VarigLonnsendring",
                                listOf(
                                    Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 1, 28)),
                                ),
                                gjelderFra = LocalDate.of(2025, 1, 20),
                                bleKjent = LocalDate.of(2025, 1, 20),
                            ),
                        ),
                    begrunnelseForReduksjonEllerIkkeUtbetalt = "BetvilerArbeidsufoerhet",
                    nærRelasjon = true,
                    opphørAvNaturalytelser =
                        listOf(
                            OpphoerAvNaturalytelse(
                                naturalytelse = Naturalytelse.ANNET,
                                fom = LocalDate.of(2025, 1, 1),
                                beloepPrMnd = BigDecimal("2000.00"),
                            ),
                        ),
                    gjenopptakelseNaturalytelser =
                        listOf(
                            GjenopptakelseNaturalytelse(
                                naturalytelse = Naturalytelse.ANNET,
                                fom = LocalDate.of(2025, 1, 10),
                                beloepPrMnd = BigDecimal("2000.00"),
                            ),
                        ),
                ),
                skapInntektsmelding(
                    inntektsmeldingId = UUID.randomUUID().toString(),
                    organisasjon = kranførerkompaniet,
                    månedsinntekt = 89000.0,
                    arbeidstakerFnr = kalleFnr,
                ),
            ),
        behandlinger =
            listOf(
                Behandling(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 2, 28),
                    søknadIder = setOf(SOKNAD_ID_3, SOKNAD_ID_4, SOKNAD_ID_5),
                ),
                Behandling(
                    fom = LocalDate.of(2024, 8, 2),
                    tom = LocalDate.of(2024, 8, 9),
                    søknadIder = setOf(SOKNAD_ID_1),
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
                    id = SOKNAD_ID_1
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2024, 8, 10),
                    tom = LocalDate.of(2024, 9, 22),
                ) {
                    id = SOKNAD_ID_2
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                ) {
                    id = SOKNAD_ID_3
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 2, 1),
                    tom = LocalDate.of(2025, 2, 28),
                ) {
                    id = SOKNAD_ID_4
                    arbeidsgiverNavn = "Kranførerkompaniet"
                    arbeidsgiverOrgnummer = "987654321"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                ) {
                    id = SOKNAD_ID_5
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                ) {
                    id = SOKNAD_ID_6
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 31),
                ) {
                    id = SOKNAD_ID_7
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 9, 1),
                    tom = LocalDate.of(2025, 9, 20),
                ) {
                    id = SOKNAD_ID_8
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
                soknad(
                    fnr = kalleFnr,
                    fom = LocalDate.of(2025, 9, 21),
                    tom = LocalDate.of(2025, 9, 30),
                ) {
                    id = SOKNAD_ID_9
                    arbeidsgiverNavn = "Krankompisen"
                    arbeidsgiverOrgnummer = "123456789"
                    arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER
                },
            ),
    )
