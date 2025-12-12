package no.nav.helse.bakrommet.sykepengesoknad

import no.nav.helse.flex.sykepengesoknad.kafka.SporsmalDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SvarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SvartypeDTO
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun skapUndersporsmalUke(
    uker: List<Uke>,
    periodeIndex: Int,
    valgteBehandlingsdager: List<LocalDate>?,
): List<SporsmalDTO> =
    uker
        .sortedBy { it.ukestart }
        .mapIndexed { ukeIndex, uke ->
            val sporsmalstekst =
                if (uke.ukestart.isBefore(uke.ukeslutt)) {
                    "${uke.ukestart.datoMånedÅrFormat()} - ${uke.ukeslutt.datoMånedÅrFormat()}"
                } else {
                    "${uke.ukestart.datoMånedÅrFormat()}"
                }

            // Legg til svar for valgte dager i uken
            val alleDagerIUken = genererAlleDagerIUken(uke.ukestart, uke.ukeslutt)
            val valgteDager = valgteBehandlingsdager?.toSet() ?: alleDagerIUken.toSet()
            val svar =
                alleDagerIUken
                    .filter { it in valgteDager }
                    .map { SvarDTO(verdi = it.toString()) }

            SporsmalDTO(
                tag = "ENKELTSTAENDE_BEHANDLINGSDAGER_UKE_$ukeIndex",
                sporsmalstekst = sporsmalstekst,
                svartype = SvartypeDTO.RADIO_GRUPPE_UKEKALENDER,
                min = uke.ukestart.toString(),
                max = uke.ukeslutt.toString(),
                svar = svar,
            )
        }

internal fun genererAlleDagerIUken(
    fra: LocalDate,
    til: LocalDate,
): List<LocalDate> {
    val dager = mutableListOf<LocalDate>()
    var current = fra
    while (!current.isAfter(til)) {
        dager.add(current)
        current = current.plusDays(1)
    }
    return dager
}

internal fun splittPeriodeIUker(
    fom: LocalDate,
    tom: LocalDate,
): List<Uke> {
    if (fom.isAfter(tom)) {
        throw IllegalArgumentException("Fom kan ikke være etter tom i periode $fom - $tom")
    }

    val uker = mutableListOf<Uke>()
    // Start fra første hverdag i uken som inneholder fom, men ikke tidligere enn fom
    var ukestart = maxOf(fom, fom.forsteHverdag())
    var maksIterasjoner = 1000 // Sikkerhet mot evig loop

    while (ukestart <= tom && maksIterasjoner > 0) {
        val ukeslutt = min(ukestart.fredagISammeUke(), tom)

        // Kun legg til uke hvis den overlapper med perioden
        if (ukestart <= tom && ukeslutt >= fom) {
            uker.add(Uke(ukestart, ukeslutt))
        }

        // Hvis ukeslutt er tom eller etter tom, er vi ferdig
        if (ukeslutt >= tom) {
            break
        }

        // Gå til neste mandag (alltid fremover i tid)
        val nesteMandag = ukeslutt.nesteMandag()

        // Sikkerhet: sjekk at vi faktisk beveger oss fremover
        if (nesteMandag <= ukestart) {
            throw IllegalStateException("Evig loop detektert: nesteMandag ($nesteMandag) er ikke etter ukestart ($ukestart). fom=$fom, tom=$tom, ukeslutt=$ukeslutt")
        }

        ukestart = nesteMandag
        maksIterasjoner--
    }

    if (maksIterasjoner == 0) {
        throw IllegalStateException("Maksimalt antall iterasjoner nådd i splittPeriodeIUker. fom=$fom, tom=$tom")
    }

    return uker
}

internal fun LocalDate.forsteHverdag(): LocalDate =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> this
        DayOfWeek.TUESDAY -> minusDays(1)
        DayOfWeek.WEDNESDAY -> minusDays(2)
        DayOfWeek.THURSDAY -> minusDays(3)
        DayOfWeek.FRIDAY -> minusDays(4)
        DayOfWeek.SATURDAY -> minusDays(5)
        DayOfWeek.SUNDAY -> minusDays(6)
    }

internal fun LocalDate.nesteMandag(): LocalDate =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> plusDays(7)
        DayOfWeek.TUESDAY -> plusDays(6)
        DayOfWeek.WEDNESDAY -> plusDays(5)
        DayOfWeek.THURSDAY -> plusDays(4)
        DayOfWeek.FRIDAY -> plusDays(3)
        DayOfWeek.SATURDAY -> plusDays(2)
        DayOfWeek.SUNDAY -> plusDays(1)
    }

internal fun LocalDate.fredagISammeUke(): LocalDate =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> plusDays(4)
        DayOfWeek.TUESDAY -> plusDays(3)
        DayOfWeek.WEDNESDAY -> plusDays(2)
        DayOfWeek.THURSDAY -> plusDays(1)
        DayOfWeek.FRIDAY -> this
        DayOfWeek.SATURDAY -> minusDays(1)
        DayOfWeek.SUNDAY -> minusDays(2)
    }

internal fun min(
    a: LocalDate,
    b: LocalDate,
): LocalDate = if (a.isBefore(b)) a else b

internal fun formatterPeriode(
    fom: LocalDate,
    tom: LocalDate,
): String = "${fom.datoMånedÅrFormat()} - ${tom.datoMånedÅrFormat()}"

internal fun LocalDate.datoMånedÅrFormat(): String = format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

internal data class Uke(
    val ukestart: LocalDate,
    val ukeslutt: LocalDate,
)
