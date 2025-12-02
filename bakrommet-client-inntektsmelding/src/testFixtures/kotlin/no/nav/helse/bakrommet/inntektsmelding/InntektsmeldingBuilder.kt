package no.nav.helse.bakrommet.inntektsmelding

import no.nav.helse.bakrommet.ereg.Organisasjon
import no.nav.inntektsmeldingkontrakt.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Builder for å lage Inntektsmelding-objekter for testing.
 * Gir standardverdier for de fleste felter slik at man kun trenger å spesifisere det som er relevant.
 */
class InntektsmeldingBuilder(
    private var inntektsmeldingId: String = UUID.randomUUID().toString(),
    private var arbeidstakerFnr: String = "12345678901",
    private var arbeidstakerAktorId: String = "1234567890123",
    private var virksomhetsnummer: String? = "999888777",
    private var arbeidsgiverFnr: String? = null,
    private var arbeidsgiverAktorId: String? = null,
    private var innsenderFulltNavn: String = "BEROEMT FLYTTELASS",
    private var innsenderTelefon: String = "11223344",
    private var begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
    private var bruttoUtbetalt: BigDecimal? = null,
    private var arbeidsgivertype: Arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
    private var arbeidsforholdId: String? = null,
    private var beregnetInntekt: BigDecimal? = BigDecimal("8876.00"),
    private var inntektsdato: LocalDate? = LocalDate.of(2025, 2, 1),
    private var refusjon: Refusjon = Refusjon(beloepPrMnd = BigDecimal("0.00"), opphoersdato = null),
    private var endringIRefusjoner: List<EndringIRefusjon> = emptyList(),
    private var opphoerAvNaturalytelser: List<OpphoerAvNaturalytelse> = emptyList(),
    private var gjenopptakelseNaturalytelser: List<GjenopptakelseNaturalytelse> = emptyList(),
    private var arbeidsgiverperioder: List<Periode> = listOf(Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 16))),
    private var status: Status = Status.GYLDIG,
    private var arkivreferanse: String = "im_690924579",
    private var ferieperioder: List<Periode> = emptyList(),
    private var foersteFravaersdag: LocalDate? = null,
    private var mottattDato: LocalDateTime = LocalDateTime.of(2025, 5, 5, 13, 58, 1),
    private var naerRelasjon: Boolean? = null,
    private var avsenderSystem: AvsenderSystem? = AvsenderSystem("NAV_NO", "1.0"),
    private var inntektEndringAarsak: InntektEndringAarsak? = null,
    private var inntektEndringAarsaker: List<InntektEndringAarsak>? = null,
    private var arsakTilInnsending: ArsakTilInnsending = ArsakTilInnsending.Ny,
    private var mottaksKanal: MottaksKanal? = null,
    private var format: Format? = null,
    private var forespurt: Boolean = true,
    private var vedtaksperiodeId: UUID? = null,
) {
    fun medInntektsmeldingId(id: String) = apply { this.inntektsmeldingId = id }

    fun medArbeidstakerFnr(fnr: String) = apply { this.arbeidstakerFnr = fnr }

    fun medArbeidstakerAktorId(aktorId: String) = apply { this.arbeidstakerAktorId = aktorId }

    fun medVirksomhetsnummer(orgnummer: String?) = apply { this.virksomhetsnummer = orgnummer }

    fun medArbeidsgiverFnr(fnr: String?) = apply { this.arbeidsgiverFnr = fnr }

    fun medArbeidsgiverAktorId(aktorId: String?) = apply { this.arbeidsgiverAktorId = aktorId }

    fun medInnsenderFulltNavn(navn: String) = apply { this.innsenderFulltNavn = navn }

    fun medInnsenderTelefon(telefon: String) = apply { this.innsenderTelefon = telefon }

    fun medBegrunnelseForReduksjonEllerIkkeUtbetalt(begrunnelse: String?) = apply { this.begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelse }

    fun medBruttoUtbetalt(belop: BigDecimal?) = apply { this.bruttoUtbetalt = belop }

    fun medArbeidsgivertype(type: Arbeidsgivertype) = apply { this.arbeidsgivertype = type }

    fun medArbeidsforholdId(id: String?) = apply { this.arbeidsforholdId = id }

    fun medBeregnetInntekt(inntekt: BigDecimal?) = apply { this.beregnetInntekt = inntekt }

    fun medBeregnetInntekt(inntekt: Double) = apply { this.beregnetInntekt = BigDecimal(inntekt.toString()) }

    fun medInntektsdato(dato: LocalDate?) = apply { this.inntektsdato = dato }

    fun medRefusjon(refusjon: Refusjon) = apply { this.refusjon = refusjon }

    fun medRefusjon(
        beloepPrMnd: BigDecimal?,
        opphoersdato: LocalDate? = null,
    ) = apply {
        this.refusjon = Refusjon(beloepPrMnd = beloepPrMnd, opphoersdato = opphoersdato)
    }

    fun medEndringIRefusjoner(endringer: List<EndringIRefusjon>) = apply { this.endringIRefusjoner = endringer }

    fun medOpphoerAvNaturalytelser(opphoer: List<OpphoerAvNaturalytelse>) = apply { this.opphoerAvNaturalytelser = opphoer }

    fun medGjenopptakelseNaturalytelser(gjenopptakelse: List<GjenopptakelseNaturalytelse>) = apply { this.gjenopptakelseNaturalytelser = gjenopptakelse }

    fun medArbeidsgiverperioder(perioder: List<Periode>) = apply { this.arbeidsgiverperioder = perioder }

    fun medArbeidsgiverperiode(
        fom: LocalDate,
        tom: LocalDate,
    ) = apply {
        this.arbeidsgiverperioder = listOf(Periode(fom, tom))
    }

    fun medStatus(status: Status) = apply { this.status = status }

    fun medArkivreferanse(referanse: String) = apply { this.arkivreferanse = referanse }

    fun medFerieperioder(perioder: List<Periode>) = apply { this.ferieperioder = perioder }

    fun medFoersteFravaersdag(dato: LocalDate?) = apply { this.foersteFravaersdag = dato }

    fun medMottattDato(dato: LocalDateTime) = apply { this.mottattDato = dato }

    fun medNaerRelasjon(naerRelasjon: Boolean?) = apply { this.naerRelasjon = naerRelasjon }

    fun medAvsenderSystem(system: AvsenderSystem?) = apply { this.avsenderSystem = system }

    fun medInntektEndringAarsak(aarsak: InntektEndringAarsak?) = apply { this.inntektEndringAarsak = aarsak }

    fun medInntektEndringAarsaker(aarsaker: List<InntektEndringAarsak>?) = apply { this.inntektEndringAarsaker = aarsaker }

    fun medArsakTilInnsending(arsak: ArsakTilInnsending) = apply { this.arsakTilInnsending = arsak }

    fun medMottaksKanal(kanal: MottaksKanal?) = apply { this.mottaksKanal = kanal }

    fun medFormat(format: Format?) = apply { this.format = format }

    fun medForespurt(forespurt: Boolean) = apply { this.forespurt = forespurt }

    fun medVedtaksperiodeId(id: UUID?) = apply { this.vedtaksperiodeId = id }

    fun build(): Inntektsmelding =
        Inntektsmelding(
            inntektsmeldingId = inntektsmeldingId,
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidstakerFnr = arbeidstakerFnr,
            arbeidstakerAktorId = arbeidstakerAktorId,
            virksomhetsnummer = virksomhetsnummer,
            arbeidsgiverFnr = arbeidsgiverFnr,
            arbeidsgiverAktorId = arbeidsgiverAktorId,
            innsenderFulltNavn = innsenderFulltNavn,
            innsenderTelefon = innsenderTelefon,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            bruttoUtbetalt = bruttoUtbetalt,
            arbeidsgivertype = arbeidsgivertype,
            arbeidsforholdId = arbeidsforholdId,
            beregnetInntekt = beregnetInntekt,
            inntektsdato = inntektsdato,
            refusjon = refusjon,
            endringIRefusjoner = endringIRefusjoner,
            opphoerAvNaturalytelser = opphoerAvNaturalytelser,
            gjenopptakelseNaturalytelser = gjenopptakelseNaturalytelser,
            arbeidsgiverperioder = arbeidsgiverperioder,
            status = status,
            arkivreferanse = arkivreferanse,
            ferieperioder = ferieperioder,
            foersteFravaersdag = foersteFravaersdag,
            mottattDato = mottattDato,
            naerRelasjon = naerRelasjon,
            avsenderSystem = avsenderSystem,
            inntektEndringAarsak = inntektEndringAarsak,
            inntektEndringAarsaker = inntektEndringAarsaker,
            arsakTilInnsending = arsakTilInnsending,
            mottaksKanal = mottaksKanal,
            format = format,
            forespurt = forespurt,
        )
}

fun skapInntektsmelding(
    inntektsmeldingId: String = UUID.randomUUID().toString(),
    arbeidstakerFnr: String,
    månedsinntekt: Double = 8876.00,
    foersteFravaersdag: LocalDate? = null,
    organisasjon: Organisasjon? = null,
    virksomhetsnummer: String = organisasjon?.orgnummer ?: "999888777",
    refusjon: Refusjon = Refusjon(beloepPrMnd = BigDecimal("0.00"), opphoersdato = null),
    endringIRefusjoner: List<EndringIRefusjon> = emptyList(),
    arbeidsgiverperioder: List<Periode> = listOf(Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 16))),
    ferieperioder: List<Periode> = emptyList(),
    mottattDato: LocalDateTime = LocalDateTime.of(2025, 5, 5, 13, 58, 1),
    inntektEndringÅrsaker: List<InntektEndringAarsak>? = null,
    begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
    nærRelasjon: Boolean? = null,
    opphørAvNaturalytelser: List<OpphoerAvNaturalytelse> = emptyList(),
    gjenopptakelseNaturalytelser: List<GjenopptakelseNaturalytelse> = emptyList(),
    block: (InntektsmeldingBuilder.() -> Unit)? = null,
): Inntektsmelding =
    InntektsmeldingBuilder(
        inntektsmeldingId = inntektsmeldingId,
        arbeidstakerFnr = arbeidstakerFnr,
        beregnetInntekt = BigDecimal(månedsinntekt.toString()),
        virksomhetsnummer = virksomhetsnummer,
        foersteFravaersdag = foersteFravaersdag,
        refusjon = refusjon,
        endringIRefusjoner = endringIRefusjoner,
        arbeidsgiverperioder = arbeidsgiverperioder,
        ferieperioder = ferieperioder,
        inntektEndringAarsaker = inntektEndringÅrsaker,
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
        naerRelasjon = nærRelasjon,
        opphoerAvNaturalytelser = opphørAvNaturalytelser,
        gjenopptakelseNaturalytelser = gjenopptakelseNaturalytelser,
        mottattDato = mottattDato,
    ).apply { block?.invoke(this) }.build()
