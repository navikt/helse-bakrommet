package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import java.time.LocalDate
import java.util.UUID

/**
 * Sealed class for alle feil som kan oppstå under utbetalingsberegning
 */
sealed class UtbetalingsberegningFeil : Exception() {
    data class ManglendeInntekt(
        val yrkesaktivitetId: UUID,
    ) : UtbetalingsberegningFeil() {
        override val message: String = "Fant ikke inntekt for yrkesaktivitet $yrkesaktivitetId"
    }

    data class UgyldigSykdomsgrad(
        val grad: Int,
    ) : UtbetalingsberegningFeil() {
        override val message: String = "Ugyldig sykdomsgrad: $grad. Må være mellom 0 og 100"
    }

    data class TomSaksbehandlingsperiode(
        val fom: LocalDate,
        val tom: LocalDate,
    ) : UtbetalingsberegningFeil() {
        override val message: String = "Saksbehandlingsperiode kan ikke være tom: $fom til $tom"
    }

    data class UgyldigPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
    ) : UtbetalingsberegningFeil() {
        override val message: String = "Ugyldig periode: fra $fom til $tom (fom må være før tom)"
    }

    data class NegativInntekt(
        val beløp: Long,
    ) : UtbetalingsberegningFeil() {
        override val message: String = "Inntekt kan ikke være negativ: $beløp"
    }
}
