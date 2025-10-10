package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.økonomi.Prosentdel

/**
 * Value object for arbeidsdager med standard verdier
 */
data class Arbeidsdager(
    val antall: Int,
) {
    init {
        require(antall > 0) { "Arbeidsdager må være større enn 0" }
    }

    companion object {
        val STANDARD_ÅRLIG = Arbeidsdager(260)
    }
}

/**
 * Value object for sykdomsgrad med validering
 */
data class Sykdomsgrad(
    val prosent: Int,
) {
    init {
        require(prosent in 0..100) { "Sykdomsgrad må være mellom 0 og 100" }
    }

    fun tilProsentdel(): Prosentdel = Prosentdel.gjenopprett(ProsentdelDto(prosent / 100.0))

    companion object {
        val FULL = Sykdomsgrad(100)
        val INGEN = Sykdomsgrad(0)
    }
}

/**
 * Value object for daglig inntekt med konvertering til økonomi-objekt
 */
data class DagligInntekt(
    val beløpØre: Long,
) {
    init {
        require(beløpØre >= 0) { "Inntekt kan ikke være negativ" }
    }

    fun tilKroner(): Int = (beløpØre / 100).toInt()

    companion object {
        val INGEN = DagligInntekt(0)
    }
}

/**
 * Konfigurasjon for utbetalingsberegning
 */
object UtbetalingsberegningKonfigurasjon {
    const val STANDARD_ÅRLIGE_ARBEIDSDAGER = 260
    const val ØRE_TIL_KRONER_FAKTOR = 100.0
    const val MÅNEDLIG_TIL_ÅRLIG_FAKTOR = 12
}
