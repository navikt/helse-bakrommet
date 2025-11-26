package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

class ArbeidsledigUtbetalingstidslinjeBuilderVedtaksperiode(
    private val maksInntektTilFordelingPerDag: Beløpstidslinje,
    private val dekningsgrad: Prosentdel,
) {
    private fun medInntektHvisFinnes(dato: LocalDate, grad: Prosentdel): Økonomi {
        return medInntekt(dato, grad)
    }

    private fun medInntekt(dato: LocalDate, grad: Prosentdel): Økonomi {
        return Økonomi.inntekt(
            sykdomsgrad = grad,
            aktuellDagsinntekt = (maksInntektTilFordelingPerDag[dato] as? Beløpsdag)?.beløp ?: INGEN,
            dekningsgrad = dekningsgrad,
            refusjonsbeløp = INGEN,
            inntektjustering = INGEN,
        )
    }


    fun result(
        sykdomstidslinje: Sykdomstidslinje,
    ): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                is Dag.Arbeidsdag -> arbeidsdag(builder, dag.dato)
                is Dag.ForeldetSykedag -> foreldetdag(builder, dag.dato, dag.grad)
                is Dag.FriskHelgedag -> arbeidsdag(builder, dag.dato)
                is Dag.SykHelgedag -> helg(builder, dag.dato, dag.grad)
                is Dag.Sykedag -> navDag(builder, dag.dato, dag.grad)
                is Dag.Avslått -> avvistDag(builder, dag.dato, Prosentdel.NullProsent, Begrunnelse.AvslåttSpillerom)

                is Dag.AndreYtelser -> {
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

                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.Arbeidsgiverdag,
                is Dag.ArbeidsgiverHelgedag,
                is Dag.Permisjonsdag,
                is Dag.ProblemDag,
                is Dag.Feriedag,
                is Dag.UkjentDag,
                    -> error("Forventer ikke ${dag::class.simpleName} i utbetalingstidslinjen for selvstendig næringsdrivende")
            }
        }
        return builder.build()
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

    private fun arbeidsdag(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
    ) {
        builder.addArbeidsdag(dato, medInntektHvisFinnes(dato, 0.prosent).ikkeBetalt())
    }

    private fun ventetidsdag(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
        sykdomsgrad: Prosentdel,
    ) {
        builder.addVentetidsdag(dato, medInntektHvisFinnes(dato, sykdomsgrad).ikkeBetalt())
    }

    private fun foreldetdag(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
        sykdomsgrad: Prosentdel,
    ) {
        builder.addForeldetDag(dato, medInntektHvisFinnes(dato, sykdomsgrad).ikkeBetalt())
    }

    private fun avvistDag(
        builder: Utbetalingstidslinje.Builder,
        dato: LocalDate,
        grad: Prosentdel,
        begrunnelse: Begrunnelse,
    ) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(dato, grad).ikkeBetalt(), listOf(begrunnelse))
    }
}
