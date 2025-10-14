package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningInput
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.tilSykdomstidslinje
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.ArbeidsledigUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.InaktivUtbetalingstidslinjeBuilder
import no.nav.helse.utbetalingstidslinje.SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Bygger utbetalingstidslinje for en yrkesaktivitet basert på kategorisering
 */
fun byggUtbetalingstidslinjeForYrkesaktivitet(
    yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
    dekningsgrad: ProsentdelDto,
    input: UtbetalingsberegningInput,
    refusjonstidslinje: Beløpstidslinje,
    fastsattÅrsinntekt: Inntekt,
): Utbetalingstidslinje {
    val dager = fyllUtManglendeDager(yrkesaktivitetDbRecord.dagoversikt ?: emptyList(), input.saksbehandlingsperiode)
    val arbeidsgiverperiode = yrkesaktivitetDbRecord.hentPerioderForType(Periodetype.ARBEIDSGIVERPERIODE)
    val dagerNavOvertarAnsvar = dager.tilDagerNavOvertarAnsvar()
    val sykdomstidslinje = dager.tilSykdomstidslinje(arbeidsgiverperiode)
    val inntektjusteringer = Beløpstidslinje(emptyList()) // TODO: Dette er tilkommen inntekt?

    // Valider at dagerNavOvertarAnsvar er innenfor arbeidsgiverperioden
    if (dagerNavOvertarAnsvar.any { navPeriode -> arbeidsgiverperiode.none { agp -> navPeriode in agp } }) {
        throw IllegalArgumentException("Ugyldig input: dagerNavOvertarAnsvar må være innenfor arbeidsgiverperioden")
    }

    return when (yrkesaktivitetDbRecord.kategorisering["INNTEKTSKATEGORI"]) {
        "INAKTIV" ->
            byggInaktivUtbetalingstidslinje(
                fastsattÅrsinntekt = fastsattÅrsinntekt,
                dekningsgrad = dekningsgrad,
                inntektjusteringer = inntektjusteringer,
                yrkesaktivitetDbRecord = yrkesaktivitetDbRecord,
                sykdomstidslinje = sykdomstidslinje,
            )
        "SELVSTENDIG_NÆRINGSDRIVENDE" ->
            byggSelvstendigUtbetalingstidslinje(
                fastsattÅrsinntekt = fastsattÅrsinntekt,
                dekningsgrad = dekningsgrad,
                yrkesaktivitetDbRecord = yrkesaktivitetDbRecord,
                sykdomstidslinje = sykdomstidslinje,
            )
        "ARBEIDSLEDIG" ->
            byggArbeidsledigtidslinje(
                fastsattÅrsinntekt = fastsattÅrsinntekt,
                dekningsgrad = dekningsgrad,
                sykdomstidslinje = sykdomstidslinje,
            )
        else ->
            byggArbeidstakerUtbetalingstidslinje(
                arbeidsgiverperiode = arbeidsgiverperiode,
                dekningsgrad = dekningsgrad,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                refusjonstidslinje = refusjonstidslinje,
                fastsattÅrsinntekt = fastsattÅrsinntekt,
                inntektjusteringer = inntektjusteringer,
                sykdomstidslinje = sykdomstidslinje,
            )
    }
}

/**
 * Bygger inaktiv utbetalingstidslinje
 */
private fun byggInaktivUtbetalingstidslinje(
    fastsattÅrsinntekt: Inntekt,
    dekningsgrad: ProsentdelDto,
    inntektjusteringer: Beløpstidslinje,
    yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
    sykdomstidslinje: no.nav.helse.sykdomstidslinje.Sykdomstidslinje,
): Utbetalingstidslinje =
    InaktivUtbetalingstidslinjeBuilder(
        fastsattÅrsinntekt = fastsattÅrsinntekt,
        dekningsgrad = dekningsgrad.tilProsentdel(),
        inntektjusteringer = inntektjusteringer,
        venteperiode = yrkesaktivitetDbRecord.hentPerioderForType(Periodetype.VENTETID_INAKTIV),
    ).result(sykdomstidslinje)

/**
 * Bygger selvstendig utbetalingstidslinje
 */
private fun byggSelvstendigUtbetalingstidslinje(
    fastsattÅrsinntekt: Inntekt,
    dekningsgrad: ProsentdelDto,
    yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
    sykdomstidslinje: no.nav.helse.sykdomstidslinje.Sykdomstidslinje,
): Utbetalingstidslinje =
    SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(
        fastsattÅrsinntekt = fastsattÅrsinntekt,
        dekningsgrad = dekningsgrad.tilProsentdel(),
        ventetid = yrkesaktivitetDbRecord.hentPerioderForType(Periodetype.VENTETID),
    ).result(sykdomstidslinje)

/**
 * Bygger selvstendig utbetalingstidslinje
 */
private fun byggArbeidsledigtidslinje(
    fastsattÅrsinntekt: Inntekt,
    dekningsgrad: ProsentdelDto,
    sykdomstidslinje: no.nav.helse.sykdomstidslinje.Sykdomstidslinje,
): Utbetalingstidslinje =
    ArbeidsledigUtbetalingstidslinjeBuilderVedtaksperiode(
        fastsattÅrsinntekt = fastsattÅrsinntekt,
        dekningsgrad = dekningsgrad.tilProsentdel(),
    ).result(sykdomstidslinje)

/**
 * Bygger arbeidstaker utbetalingstidslinje
 */
private fun byggArbeidstakerUtbetalingstidslinje(
    arbeidsgiverperiode: List<Periode>,
    dekningsgrad: ProsentdelDto,
    dagerNavOvertarAnsvar: List<Periode>,
    refusjonstidslinje: Beløpstidslinje,
    fastsattÅrsinntekt: Inntekt,
    inntektjusteringer: Beløpstidslinje,
    sykdomstidslinje: no.nav.helse.sykdomstidslinje.Sykdomstidslinje,
): Utbetalingstidslinje =
    ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
        arbeidsgiverperiode = arbeidsgiverperiode,
        dekningsgrad = dekningsgrad.tilProsentdel(),
        dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
        refusjonstidslinje = refusjonstidslinje,
        fastsattÅrsinntekt = fastsattÅrsinntekt,
        inntektjusteringer = inntektjusteringer,
    ).result(sykdomstidslinje)

/**
 * Oppretter refusjonstidslinje fra refusjonsdata
 */
fun opprettRefusjonstidslinjeFraData(refusjonstidslinjeData: Map<LocalDate, Inntekt>): Beløpstidslinje {
    val beløpsdager =
        refusjonstidslinjeData.map { (dato, inntekt) ->
            Beløpsdag(
                dato = dato,
                beløp = inntekt,
                kilde =
                    Kilde(
                        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                        avsender = Avsender.ARBEIDSGIVER,
                        tidsstempel = LocalDateTime.now(),
                    ),
            )
        }
    return Beløpstidslinje(beløpsdager)
}
