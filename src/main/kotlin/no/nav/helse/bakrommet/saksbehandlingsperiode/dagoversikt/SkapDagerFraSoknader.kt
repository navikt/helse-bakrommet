package no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt

import no.nav.helse.flex.sykepengesoknad.kafka.FravarstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.DayOfWeek
import java.time.LocalDate

fun skapDagoversiktFraSoknader(
    søknader: List<SykepengesoknadDTO>,
    fom: LocalDate,
    tom: LocalDate,
): List<Dag> {
    val dager = initialiserDager(fom, tom)
    val sorterteSøknader = søknader.sortedByDescending { it.sendtNav ?: it.opprettet }

    return sorterteSøknader.fold(dager) { oppdaterteDager, søknad ->
        oppdaterDagerMedSøknadsdata(oppdaterteDager, søknad, fom, tom)
    }
}

private fun initialiserDager(
    fom: LocalDate,
    tom: LocalDate,
): List<Dag> {
    return generateSequence(fom) { it.plusDays(1) }
        .takeWhile { !it.isAfter(tom) }
        .map { dato ->
            val erHelg = dato.dayOfWeek == DayOfWeek.SATURDAY || dato.dayOfWeek == DayOfWeek.SUNDAY
            val dagtype = if (erHelg) Dagtype.Helg else Dagtype.Arbeidsdag

            Dag(
                dato = dato,
                dagtype = dagtype,
                grad = null,
                avvistBegrunnelse = emptyList(),
                kilde = Kilde.Saksbehandler,
            )
        }
        .toList()
}

private fun oppdaterDagerMedSøknadsdata(
    dager: List<Dag>,
    søknad: SykepengesoknadDTO,
    fom: LocalDate,
    tom: LocalDate,
): List<Dag> {
    val dagerMap = dager.associateBy { it.dato }.toMutableMap()

    // Legg til sykedager fra søknadsperioder
    søknad.soknadsperioder?.forEach { periode ->
        oppdaterDagerMedPeriode(dagerMap, periode, fom, tom, Dagtype.Syk) {
            periode.faktiskGrad ?: periode.grad ?: periode.sykmeldingsgrad
        }
    }

    // Legg til permisjon fra fraværslisten
    søknad.fravar?.filter { it.type == FravarstypeDTO.PERMISJON }?.forEach { fravær ->
        oppdaterDagerMedFravær(dagerMap, fravær, fom, tom, Dagtype.Permisjon)
    }

    // Legg til ferie fra fraværslisten (tar presedens over permisjon)
    søknad.fravar?.filter { it.type == FravarstypeDTO.FERIE }?.forEach { fravær ->
        oppdaterDagerMedFravær(dagerMap, fravær, fom, tom, Dagtype.Ferie)
    }

    return dagerMap.values.toList()
}

private fun oppdaterDagerMedPeriode(
    dagerMap: MutableMap<LocalDate, Dag>,
    periode: no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO,
    fom: LocalDate,
    tom: LocalDate,
    dagtype: Dagtype,
    gradHenter: () -> Int? = { null },
) {
    val periodeFom = periode.fom ?: return
    val periodeTom = periode.tom ?: return

    val overlappendeFom = maxOf(fom, periodeFom)
    val overlappendeTom = minOf(tom, periodeTom)

    if (!overlappendeFom.isAfter(overlappendeTom)) {
        oppdaterDagerIIntervall(dagerMap, overlappendeFom, overlappendeTom, dagtype, gradHenter())
    }
}

private fun oppdaterDagerMedFravær(
    dagerMap: MutableMap<LocalDate, Dag>,
    fravær: no.nav.helse.flex.sykepengesoknad.kafka.FravarDTO,
    fom: LocalDate,
    tom: LocalDate,
    dagtype: Dagtype,
    gradHenter: () -> Int? = { null },
) {
    val fraværFom = fravær.fom ?: return
    val fraværTom = fravær.tom ?: fraværFom

    val overlappendeFom = maxOf(fom, fraværFom)
    val overlappendeTom = minOf(tom, fraværTom)

    if (!overlappendeFom.isAfter(overlappendeTom)) {
        oppdaterDagerIIntervall(dagerMap, overlappendeFom, overlappendeTom, dagtype, gradHenter())
    }
}

private fun oppdaterDagerIIntervall(
    dagerMap: MutableMap<LocalDate, Dag>,
    fom: LocalDate,
    tom: LocalDate,
    dagtype: Dagtype,
    grad: Int?,
) {
    generateSequence(fom) { it.plusDays(1) }
        .takeWhile { !it.isAfter(tom) }
        .forEach { dato ->
            val eksisterendeDag = dagerMap[dato]
            if (eksisterendeDag != null && eksisterendeDag.dagtype != Dagtype.Helg) {
                dagerMap[dato] =
                    eksisterendeDag.copy(
                        dagtype = dagtype,
                        grad = grad,
                        kilde = Kilde.Søknad,
                    )
            }
        }
}
