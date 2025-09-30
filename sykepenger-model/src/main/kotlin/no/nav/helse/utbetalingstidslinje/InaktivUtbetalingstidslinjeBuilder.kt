package no.nav.helse.utbetalingstidslinje

import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate



class InaktivUtbetalingstidslinjeBuilder(
    private val fastsattÅrsinntekt: Inntekt,
    private val dekningsgrad: Prosentdel,
    private val inntektjusteringer: Beløpstidslinje,
    private val venteperiode: List<Periode>,

    ) {
    internal fun medInntektHvisFinnes(
        dato: LocalDate,
        grad: Prosentdel,
    ): Økonomi {
        return medInntekt(dato, grad)
    }

    private fun medInntekt(
        dato: LocalDate,
        grad: Prosentdel,
    ): Økonomi {
        return Økonomi.inntekt(
            sykdomsgrad = grad,
            aktuellDagsinntekt = fastsattÅrsinntekt,
            dekningsgrad = dekningsgrad,
            refusjonsbeløp = INGEN,
            inntektjustering = (inntektjusteringer[dato] as? Beløpsdag)?.beløp ?: INGEN,
        )
    }

    fun result(sykdomstidslinje: Sykdomstidslinje): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                /** <potensielt arbeidsgiverperiode-dager> **/
                is Dag.ArbeidsgiverHelgedag -> {
                    if (erVenteperiode(dag.dato)) {
                        arbeidsgiverperiodedag(builder, dag.dato, dag.grad)
                    } else {
                        helg(builder, dag.dato, dag.grad)
                    }
                }

                is Dag.Arbeidsgiverdag -> {
                    if (erVenteperiode(dag.dato)) {
                        arbeidsgiverperiodedagEllerNavAnsvar(builder, dag.dato, dag.grad)
                    } else {
                        avvistDag(builder, dag.dato, dag.grad, Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode)
                    }
                }

                is Dag.Sykedag -> {
                    if (erVenteperiode(dag.dato)) {
                        arbeidsgiverperiodedagEllerNavAnsvar(builder, dag.dato, dag.grad)
                    } else {
                        navDag(builder, dag.dato, dag.grad)
                    }
                }

                is Dag.SykHelgedag -> {
                    if (erVenteperiode(dag.dato)) {
                        arbeidsgiverperiodedag(builder, dag.dato, dag.grad)
                    } else {
                        helg(builder, dag.dato, dag.grad)
                    }
                }

                is Dag.AndreYtelser -> {
                    // andreytelse-dagen er fridag hvis den overlapper med en agp-dag, eller om vedtaksperioden ikke har noen agp -- fordi andre ytelsen spiser opp alt
                    if (erVenteperiode(dag.dato)) {
                        arbeidsgiverperiodedag(builder, dag.dato, 0.prosent)
                    } else if (venteperiode.isEmpty() || dag.dato < venteperiode.first().start) {
                        fridag(builder, dag.dato)
                    } else {
                        val begrunnelse =
                            when (dag.ytelse) {
                                Dag.AndreYtelser.AnnenYtelse.AAP -> Begrunnelse.AndreYtelserAap
                                Dag.AndreYtelser.AnnenYtelse.Dagpenger -> Begrunnelse.AndreYtelserDagpenger
                                Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> Begrunnelse.AndreYtelserForeldrepenger
                                Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> Begrunnelse.AndreYtelserOmsorgspenger
                                Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> Begrunnelse.AndreYtelserOpplaringspenger
                                Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> Begrunnelse.AndreYtelserPleiepenger
                                Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> Begrunnelse.AndreYtelserSvangerskapspenger
                            }
                        avvistDag(builder, dag.dato, 0.prosent, begrunnelse)
                    }
                }

                is Dag.Feriedag -> {
                    if (erVenteperiode(dag.dato)) {
                        arbeidsgiverperiodedag(builder, dag.dato, 0.prosent)
                    } else {
                        fridag(builder, dag.dato)
                    }
                }

                is Dag.ForeldetSykedag -> {
                    if (erVenteperiode(dag.dato)) {
                        arbeidsgiverperiodedag(builder, dag.dato, dag.grad)
                    } else {
                        builder.addForeldetDag(dag.dato, medInntektHvisFinnes(dag.dato, dag.grad).ikkeBetalt())
                    }
                }

                is Dag.ArbeidIkkeGjenopptattDag -> {
                    if (erVenteperiode(dag.dato)) {
                        arbeidsgiverperiodedag(builder, dag.dato, 0.prosent)
                    } else {
                        fridag(builder, dag.dato)
                    }
                }

                is Dag.Permisjonsdag -> {
                    if (erVenteperiode(dag.dato)) {
                        arbeidsgiverperiodedag(builder, dag.dato, 0.prosent)
                    } else {
                        fridag(builder, dag.dato)
                    }
                }
                /** </potensielt arbeidsgiverperiode-dager> **/

                is Dag.Arbeidsdag -> arbeidsdag(builder, dag.dato)
                is Dag.FriskHelgedag -> arbeidsdag(builder, dag.dato)
                is Dag.ProblemDag -> {
                    // den andre builderen kaster egentlig exception her, men trenger vi det –– sånn egentlig?
                    fridag(builder, dag.dato)
                }

                is Dag.UkjentDag -> {
                    // todo: pga strekking av egenmeldingsdager fra søknad så har vi vedtaksperioder med ukjentdager
                    // error("Forventer ikke å finne en ukjentdag i en vedtaksperiode")
                    when (dag.dato.erHelg()) {
                        true -> fridag(builder, dag.dato)
                        false -> arbeidsdag(builder, dag.dato)
                    }
                }
            }
        }
        return builder.build()
    }


    private fun erVenteperiode(dato: LocalDate) = venteperiode.any { dato in it }


    private fun arbeidsgiverperiodedagEllerNavAnsvar(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
        grad: Prosentdel,
    ) {
        builder.addArbeidsgiverperiodedag(dato, medInntektHvisFinnes(dato, grad).ikkeBetalt())
    }


    private fun arbeidsgiverperiodedag(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
        grad: Prosentdel,
    ) {
        builder.addArbeidsgiverperiodedag(dato, medInntektHvisFinnes(dato, grad).ikkeBetalt())
    }

    private fun avvistDag(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
        grad: Prosentdel,
        begrunnelse: Begrunnelse,
    ) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(dato, grad).ikkeBetalt(), listOf(begrunnelse))
    }

    private fun helg(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
        grad: Prosentdel,
    ) {
        builder.addHelg(dato, medInntektHvisFinnes(dato, grad).ikkeBetalt())
    }

    private fun navDag(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
        grad: Prosentdel,
    ) {
        builder.addNAVdag(dato, medInntektHvisFinnes(dato, grad))
    }

    private fun fridag(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
    ) {
        builder.addFridag(dato, medInntektHvisFinnes(dato, 0.prosent).ikkeBetalt())
    }

    private fun arbeidsdag(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
    ) {
        builder.addArbeidsdag(dato, medInntektHvisFinnes(dato, 0.prosent).ikkeBetalt())
    }
}
