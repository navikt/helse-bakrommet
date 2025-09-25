package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import kotlin.collections.List

internal fun List<Dag>.tilSykdomstidslinje(): Sykdomstidslinje {
    val kilde_HARDKODET = Hendelseskilde.INGEN

    var syk_TMP = false

    fun syk() {
        syk_TMP = true
    }

    fun frisk() {
        syk_TMP = false
    }

    fun erSyk() = syk_TMP

    val annenytelse_HARDKODET = no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Foreldrepenger

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
                        no.nav.helse.sykdomstidslinje.Dag.Arbeidsgiverdag(
                            dato = spilleromDag.dato,
                            grad = spilleromDag.grad!!.somProsentdel(),
                            kilde = kilde_HARDKODET,
                        )
                    } else {
                        no.nav.helse.sykdomstidslinje.Dag.Sykedag(
                            dato = spilleromDag.dato,
                            grad = spilleromDag.grad!!.somProsentdel(),
                            kilde = kilde_HARDKODET,
                        )
                    }.also { syk() }

                Dagtype.Arbeidsdag ->
                    no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag(
                        dato = spilleromDag.dato,
                        kilde = kilde_HARDKODET,
                    ).also { frisk() }

                Dagtype.Helg ->
                    if (erSyk()) {
                        if (spilleromDag.dato.erAGP()) {
                            no.nav.helse.sykdomstidslinje.Dag.ArbeidsgiverHelgedag(
                                dato = spilleromDag.dato,
                                grad = spilleromDag.grad!!.somProsentdel(),
                                kilde = kilde_HARDKODET,
                            )
                        } else {
                            no.nav.helse.sykdomstidslinje.Dag.SykHelgedag(
                                dato = spilleromDag.dato,
                                grad = spilleromDag.grad!!.somProsentdel(),
                                kilde = kilde_HARDKODET,
                            )
                        }
                    } else {
                        no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag(
                            dato = spilleromDag.dato,
                            kilde = kilde_HARDKODET,
                        )
                    }

                Dagtype.Ferie ->
                    no.nav.helse.sykdomstidslinje.Dag.Feriedag(
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
                        no.nav.helse.sykdomstidslinje.Dag.ForeldetSykedag(
                            dato = spilleromDag.dato,
                            grad = spilleromDag.grad!!.somProsentdel(),
                            kilde = kilde_HARDKODET,
                        )
                    } else {
                        no.nav.helse.sykdomstidslinje.Dag.ArbeidIkkeGjenopptattDag(
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

                Dagtype.Ventetid -> throw IllegalArgumentException("Ventetid bør være metadata?")
            }.let { spleisDag ->
                spilleromDag.dato to spleisDag
            }
        }.let {
            mapOf(*it.toTypedArray())
        }

    return Sykdomstidslinje(spleisDagerMap)
}
