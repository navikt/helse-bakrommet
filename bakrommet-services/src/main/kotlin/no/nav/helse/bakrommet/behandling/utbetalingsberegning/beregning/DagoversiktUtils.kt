package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningFeil
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.hendelser.Periode
import java.time.LocalDate

fun fyllUtManglendeDager(
    eksisterendeDager: List<Dag>,
    saksbehandlingsperiode: PeriodeDto,
): List<Dag> {
    validerPeriode(saksbehandlingsperiode)

    val eksisterendeDatoer = eksisterendeDager.map { it.dato }.toSet()
    val komplettDagoversikt = mutableListOf<Dag>()

    // Legg til alle eksisterende dager
    komplettDagoversikt.addAll(eksisterendeDager)

    // Fyll ut manglende dager som arbeidsdager
    var aktuellDato = saksbehandlingsperiode.fom
    while (!aktuellDato.isAfter(saksbehandlingsperiode.tom)) {
        if (!eksisterendeDatoer.contains(aktuellDato)) {
            val arbeidsdag = opprettArbeidsdag(aktuellDato)
            komplettDagoversikt.add(arbeidsdag)
        }
        aktuellDato = aktuellDato.plusDays(1)
    }

    // Sorter dager etter dato
    return komplettDagoversikt.sortedBy { it.dato }
}

/**
 * Konverterer en liste med dager til perioder hvor NAV overtar ansvar
 */
fun List<Dag>?.tilDagerNavOvertarAnsvar(): List<Periode> {
    // Alle dager som er SykNav er dager NAV overtar ansvar
    if (this == null) return emptyList()
    val sykNavDager = this.filter { it.dagtype == Dagtype.SykNav }.map { it.dato }.toSet()
    if (sykNavDager.isEmpty()) return emptyList()

    val sortedDager = sykNavDager.sorted()
    val perioder = mutableListOf<Periode>()
    var periodeStart = sortedDager.first()
    var periodeSlutt = sortedDager.first()

    for (i in 1 until sortedDager.size) {
        val currentDate = sortedDager[i]
        if (currentDate == periodeSlutt.plusDays(1)) {
            // Fortsett perioden
            periodeSlutt = currentDate
        } else {
            // Avslutt nåværende periode
            perioder.add(Periode(periodeStart, periodeSlutt))
            // Start ny periode
            periodeStart = currentDate
            periodeSlutt = currentDate
        }
    }
    // Legg til siste periode
    perioder.add(Periode(periodeStart, periodeSlutt))
    return perioder
}

/**
 * Validerer at en saksbehandlingsperiode er gyldig
 */
private fun validerPeriode(saksbehandlingsperiode: PeriodeDto) {
    if (saksbehandlingsperiode.fom.isAfter(saksbehandlingsperiode.tom)) {
        throw UtbetalingsberegningFeil.UgyldigPeriode(
            saksbehandlingsperiode.fom,
            saksbehandlingsperiode.tom,
        )
    }
}

/**
 * Oppretter en arbeidsdag for en gitt dato
 */
private fun opprettArbeidsdag(dato: LocalDate): Dag =
    Dag(
        dato = dato,
        dagtype = Dagtype.Arbeidsdag,
        grad = null,
        avslåttBegrunnelse = emptyList(),
        kilde = null,
    )
