package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.domain.sykepenger.Dag
import no.nav.helse.bakrommet.domain.sykepenger.Dagtype
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsgiverdag
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Permisjonsdag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.NullProsent
import java.time.LocalDate
import kotlin.collections.List

val kilde_HARDKODET = Hendelseskilde.INGEN
val annenytelse_HARDKODET = AndreYtelser.AnnenYtelse.Foreldrepenger

internal fun List<Dag>.tilSykdomstidslinje(arbeidsgiverperiode: List<Periode>): Sykdomstidslinje {
    fun Int.somProsentdel() = Prosentdel.gjenopprett(ProsentdelDto(this.toDouble() / 100))

    fun LocalDate.erAGP(): Boolean = arbeidsgiverperiode.any { agp -> agp.contains(this) }

    val spleisDagerMap =
        this
            .map { spilleromDag ->

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
                    Dagtype.Avslått -> {
                        throw IllegalStateException("Avslåtte dager skal ikke være en del av sykdomstidslinjen")
                    }

                    Dagtype.Syk,
                    Dagtype.SykNav,
                    Dagtype.Behandlingsdag,
                    -> {
                        val grad =
                            if (spilleromDag.dagtype == Dagtype.Behandlingsdag) {
                                100
                            } else {
                                spilleromDag.grad ?: throw IllegalArgumentException("Grad må være satt for dagtype ${spilleromDag.dagtype} på dato ${spilleromDag.dato}")
                            }

                        if (spilleromDag.dato.erAGP()) {
                            Arbeidsgiverdag(
                                dato = spilleromDag.dato,
                                grad = grad.somProsentdel(),
                                kilde = kilde_HARDKODET,
                            )
                        } else {
                            Sykedag(
                                dato = spilleromDag.dato,
                                grad = grad.somProsentdel(),
                                kilde = kilde_HARDKODET,
                            )
                        }
                    }

                    Dagtype.Arbeidsdag -> {
                        Arbeidsdag(
                            dato = spilleromDag.dato,
                            kilde = kilde_HARDKODET,
                        )
                    }

                    Dagtype.Ferie -> {
                        Feriedag(
                            dato = spilleromDag.dato,
                            kilde = kilde_HARDKODET,
                        )
                    }

                    Dagtype.Permisjon -> {
                        Permisjonsdag(
                            dato = spilleromDag.dato,
                            kilde = kilde_HARDKODET,
                        )
                    }

                    Dagtype.AndreYtelser -> {
                        AndreYtelser(
                            dato = spilleromDag.dato,
                            kilde = kilde_HARDKODET,
                            ytelse = annenytelse_HARDKODET,
                        )
                    }
                }.let { spleisDag ->
                    spilleromDag.dato to spleisDag
                }
            }.let {
                mapOf(*it.toTypedArray())
            }

    return Sykdomstidslinje(spleisDagerMap)
}
