package no.nav.helse.bakrommet.domain.sykepenger

import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    fun omslutter(other: Periode): Boolean = fom <= other.fom && tom >= other.tom
}
