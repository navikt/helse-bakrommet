package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsgiverdag
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.NullProsent
import java.time.LocalDate
import kotlin.collections.List

val kilde_HARDKODET = Hendelseskilde.INGEN
val annenytelse_HARDKODET = no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Foreldrepenger

internal fun List<Dag>.tilSykdomstidslinje(arbeidsgiverperiode: List<Periode>): Sykdomstidslinje {
    fun Int.somProsentdel() = Prosentdel.gjenopprett(ProsentdelDto(this.toDouble() / 100))

    fun LocalDate.erAGP(): Boolean {
        return arbeidsgiverperiode.any { agp -> agp.contains(this) }
    }

    val spleisDagerMap =
        this.map { spilleromDag ->

            if (spilleromDag.dato.erHelg()) {
                if (spilleromDag.dagtype == Dagtype.Syk || spilleromDag.dagtype == Dagtype.SykNav) {
                    return@map SykHelgedag(
                        dato = spilleromDag.dato,
                        grad = spilleromDag.grad?.somProsentdel() ?: NullProsent,
                        kilde = kilde_HARDKODET,
                    ).let { spleisDag ->
                        spilleromDag.dato to spleisDag
                    }
                }
                return@map FriskHelgedag(
                    dato = spilleromDag.dato,
                    kilde = kilde_HARDKODET,
                ).let { spleisDag ->
                    spilleromDag.dato to spleisDag
                }
            }

            when (spilleromDag.dagtype) {
                Dagtype.Syk,
                Dagtype.SykNav,
                    ->
                    if (spilleromDag.dato.erAGP()) {
                        Arbeidsgiverdag(
                            dato = spilleromDag.dato,
                            grad = spilleromDag.grad!!.somProsentdel(),
                            kilde = kilde_HARDKODET,
                        )
                    } else {
                        Sykedag(
                            dato = spilleromDag.dato,
                            grad = spilleromDag.grad!!.somProsentdel(),
                            kilde = kilde_HARDKODET,
                        )
                    }

                Dagtype.Arbeidsdag ->
                    Arbeidsdag(
                        dato = spilleromDag.dato,
                        kilde = kilde_HARDKODET,
                    )

                Dagtype.Ferie ->
                    Feriedag(
                        dato = spilleromDag.dato,
                        kilde = kilde_HARDKODET,
                    )

                Dagtype.Permisjon ->
                    no.nav.helse.sykdomstidslinje.Dag.Permisjonsdag(
                        dato = spilleromDag.dato,
                        kilde = kilde_HARDKODET,
                    )

                Dagtype.Avslått ->
                    // TODO eller bruke en felles avslått-dag i Sykdomstidslinje?
                    // TODO eller bare ikke ha dagen i tidslinjen?
                    UkjentDag(
                        dato = spilleromDag.dato,
                        kilde = kilde_HARDKODET,
                    )

                Dagtype.AndreYtelser ->
                    no.nav.helse.sykdomstidslinje.Dag.AndreYtelser(
                        dato = spilleromDag.dato,
                        kilde = kilde_HARDKODET,
                        ytelse = annenytelse_HARDKODET,
                    )
            }.let { spleisDag ->
                spilleromDag.dato to spleisDag
            }
        }.let {
            mapOf(*it.toTypedArray())
        }

    return Sykdomstidslinje(spleisDagerMap)
}
