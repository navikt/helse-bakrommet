package no.nav.helse.bakrommet.domain.sykepenger

import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    fun omslutter(other: Periode): Boolean = fom <= other.fom && tom >= other.tom

    fun erTilstøtendeIForkantAv(other: Periode): Boolean = tom.plusDays(1).isEqual(other.fom)

    fun erTilstøtendeIBakkantAv(other: Periode): Boolean = other.erTilstøtendeIForkantAv(this)

    fun overlapperMed(other: Periode): Boolean = other.fom in fom..tom || other.tom in fom..tom || omslutter(other) || other.omslutter(this)
}
