package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Dag.ArbeidIkkeGjenopptattDag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.ArbeidsgiverHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsgiverdag
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.sykdomstidslinje.Dag.ForeldetSykedag
import no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import kotlin.collections.List

val kilde_HARDKODET = Hendelseskilde.INGEN
val annenytelse_HARDKODET = no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Foreldrepenger

internal fun List<Dag>.tilSykdomstidslinje(): Sykdomstidslinje {
    // TODO TMP
    var syk = false

    fun syk() {
        syk = true
    }

    fun frisk() {
        syk = false
    }

    fun erSyk() = syk

    fun Int.somProsentdel() = Prosentdel.gjenopprett(ProsentdelDto(this.toDouble() / 100))

    fun LocalDate.erAGP() = false // TODO

    fun LocalDate.erForeldet() = false // TODO

    val spleisDagerMap =
        this.map { spilleromDag ->
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
                    }.also { syk() }

                Dagtype.Arbeidsdag ->
                    Arbeidsdag(
                        dato = spilleromDag.dato,
                        kilde = kilde_HARDKODET,
                    ).also { frisk() }

                Dagtype.Helg ->
                    if (erSyk()) {
                        if (spilleromDag.dato.erAGP()) {
                            ArbeidsgiverHelgedag(
                                dato = spilleromDag.dato,
                                grad = spilleromDag.grad!!.somProsentdel(),
                                kilde = kilde_HARDKODET,
                            )
                        } else {
                            SykHelgedag(
                                dato = spilleromDag.dato,
                                grad = spilleromDag.grad!!.somProsentdel(),
                                kilde = kilde_HARDKODET,
                            )
                        }
                    } else {
                        FriskHelgedag(
                            dato = spilleromDag.dato,
                            kilde = kilde_HARDKODET,
                        )
                    }

                Dagtype.Ferie ->
                    Feriedag(
                        dato = spilleromDag.dato,
                        kilde = kilde_HARDKODET,
                    ).also { frisk() }

                Dagtype.Permisjon ->
                    no.nav.helse.sykdomstidslinje.Dag.Permisjonsdag(
                        dato = spilleromDag.dato,
                        kilde = kilde_HARDKODET,
                    ).also { frisk() }

                Dagtype.Avslått ->
                    if (spilleromDag.dato.erForeldet()) {
                        // TODO: Flere enn ForeldetSykedag + ArbeidIkkeGjenopptattDag ??
                        ForeldetSykedag(
                            dato = spilleromDag.dato,
                            grad = spilleromDag.grad!!.somProsentdel(),
                            kilde = kilde_HARDKODET,
                        )
                    } else {
                        // TODO eller bruke en felles avslått-dag i Sykdomstidslinje?
                        // TODO eller bare ikke ha dagen i tidslinjen?
                        ArbeidIkkeGjenopptattDag(
                            dato = spilleromDag.dato,
                            kilde = kilde_HARDKODET,
                        )
                    }

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
