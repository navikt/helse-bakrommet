package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningInput
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.tilSykdomstidslinje
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.ArbeidsledigUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.InaktivUtbetalingstidslinjeBuilder
import no.nav.helse.utbetalingstidslinje.SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Bygger utbetalingstidslinje for en yrkesaktivitet basert på kategorisering
 */
fun byggUtbetalingstidslinjeForYrkesaktivitet(
    legacyYrkesaktivitet: LegacyYrkesaktivitet,
    dekningsgrad: ProsentdelDto,
    input: UtbetalingsberegningInput,
    refusjonstidslinje: Beløpstidslinje,
    maksInntektTilFordelingPerDag: Beløpstidslinje,
    inntektjusteringer: Beløpstidslinje,
): Utbetalingstidslinje {
    val dager = fyllUtManglendeDager(legacyYrkesaktivitet.dagoversikt?.sykdomstidlinje ?: emptyList(), input.saksbehandlingsperiode)
    val arbeidsgiverperiode = legacyYrkesaktivitet.hentPerioderForType(Periodetype.ARBEIDSGIVERPERIODE)
    val dagerNavOvertarAnsvar = dager.tilDagerNavOvertarAnsvar()
    val sykdomstidslinje = dager.tilSykdomstidslinje(arbeidsgiverperiode)

    // Valider at dagerNavOvertarAnsvar er innenfor arbeidsgiverperioden
    if (dagerNavOvertarAnsvar.any { navPeriode -> arbeidsgiverperiode.none { agp -> navPeriode in agp } }) {
        throw IllegalArgumentException("Ugyldig input: dagerNavOvertarAnsvar må være innenfor arbeidsgiverperioden")
    }

    return when (legacyYrkesaktivitet.kategorisering) {
        is YrkesaktivitetKategorisering.Inaktiv -> {
            byggInaktivUtbetalingstidslinje(
                maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
                dekningsgrad = dekningsgrad,
                inntektjusteringer = inntektjusteringer,
                legacyYrkesaktivitet = legacyYrkesaktivitet,
                sykdomstidslinje = sykdomstidslinje,
            )
        }

        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
            byggSelvstendigUtbetalingstidslinje(
                maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
                dekningsgrad = dekningsgrad,
                legacyYrkesaktivitet = legacyYrkesaktivitet,
                sykdomstidslinje = sykdomstidslinje,
            )
        }

        is YrkesaktivitetKategorisering.Arbeidsledig -> {
            byggArbeidsledigtidslinje(
                maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
                dekningsgrad = dekningsgrad,
                sykdomstidslinje = sykdomstidslinje,
            )
        }

        // TODO egen frilanser builder
        is YrkesaktivitetKategorisering.Frilanser -> {
            byggArbeidstakerUtbetalingstidslinje(
                arbeidsgiverperiode = arbeidsgiverperiode,
                dekningsgrad = dekningsgrad,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                refusjonstidslinje = refusjonstidslinje,
                maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
                inntektjusteringer = inntektjusteringer,
                sykdomstidslinje = sykdomstidslinje,
            )
        }

        is YrkesaktivitetKategorisering.Arbeidstaker -> {
            byggArbeidstakerUtbetalingstidslinje(
                arbeidsgiverperiode = arbeidsgiverperiode,
                dekningsgrad = dekningsgrad,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                refusjonstidslinje = refusjonstidslinje,
                maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
                inntektjusteringer = inntektjusteringer,
                sykdomstidslinje = sykdomstidslinje,
            )
        }
    }.avslåDager(legacyYrkesaktivitet.dagoversikt?.avslagsdager?.map { it.dato })
}

private fun Utbetalingstidslinje.avslåDager(avslagsdager: List<LocalDate>?): Utbetalingstidslinje {
    if (avslagsdager == null || avslagsdager.isEmpty()) return this

    val avslagsdagerSet = avslagsdager.toSet()
    return Utbetalingstidslinje(
        this.map { dag ->
            if (dag.dato in avslagsdagerSet) {
                AvvistDag(dag.dato, dag.økonomi, listOf(Begrunnelse.AvslåttSpillerom))
            } else {
                dag
            }
        },
    )
}

/**
 * Bygger inaktiv utbetalingstidslinje
 */
private fun byggInaktivUtbetalingstidslinje(
    maksInntektTilFordelingPerDag: Beløpstidslinje,
    dekningsgrad: ProsentdelDto,
    inntektjusteringer: Beløpstidslinje,
    legacyYrkesaktivitet: LegacyYrkesaktivitet,
    sykdomstidslinje: no.nav.helse.sykdomstidslinje.Sykdomstidslinje,
): Utbetalingstidslinje =
    InaktivUtbetalingstidslinjeBuilder(
        maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
        dekningsgrad = dekningsgrad.tilProsentdel(),
        inntektjusteringer = inntektjusteringer,
        venteperiode = legacyYrkesaktivitet.hentPerioderForType(Periodetype.VENTETID_INAKTIV),
    ).result(sykdomstidslinje)

/**
 * Bygger selvstendig utbetalingstidslinje
 */
private fun byggSelvstendigUtbetalingstidslinje(
    maksInntektTilFordelingPerDag: Beløpstidslinje,
    dekningsgrad: ProsentdelDto,
    legacyYrkesaktivitet: LegacyYrkesaktivitet,
    sykdomstidslinje: no.nav.helse.sykdomstidslinje.Sykdomstidslinje,
): Utbetalingstidslinje =
    SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(
        maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
        dekningsgrad = dekningsgrad.tilProsentdel(),
        ventetid = legacyYrkesaktivitet.hentPerioderForType(Periodetype.VENTETID),
    ).result(sykdomstidslinje)

/**
 * Bygger selvstendig utbetalingstidslinje
 */
private fun byggArbeidsledigtidslinje(
    maksInntektTilFordelingPerDag: Beløpstidslinje,
    dekningsgrad: ProsentdelDto,
    sykdomstidslinje: no.nav.helse.sykdomstidslinje.Sykdomstidslinje,
): Utbetalingstidslinje =
    ArbeidsledigUtbetalingstidslinjeBuilderVedtaksperiode(
        maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
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
    maksInntektTilFordelingPerDag: Beløpstidslinje,
    inntektjusteringer: Beløpstidslinje,
    sykdomstidslinje: no.nav.helse.sykdomstidslinje.Sykdomstidslinje,
): Utbetalingstidslinje =
    ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
        arbeidsgiverperiode = arbeidsgiverperiode,
        dekningsgrad = dekningsgrad.tilProsentdel(),
        dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
        refusjonstidslinje = refusjonstidslinje,
        maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
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
