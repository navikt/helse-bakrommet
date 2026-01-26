package no.nav.helse.bakrommet.e2e.testutils.prettyprint

import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.BeregningResponseDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.YrkesaktivitetUtbetalingsberegningDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.ØkonomiDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.maybeOrgnummer
import no.nav.helse.bakrommet.e2e.testutils.ScenarioData
import no.nav.helse.erHelg
import java.util.*

fun ScenarioData.prettyPrint() {
    val yaIdToNameMap =
        mapOf(
            *this.yrkesaktiviteter
                .map {
                    it.id to (it.kategorisering.maybeOrgnummer() ?: it.kategorisering::class.simpleName!!)
                }.toTypedArray(),
        )
    this.utbetalingsberegning?.prettyPrint(yaIdToNameMap, margin = 2)
}

fun BeregningResponseDto.prettyPrint(
    yaIdToNameMap: Map<UUID, String>? = null,
    margin: Int = 1,
) {
    val (fom, tom) = fomTom()

    val widthPerYa = 12 + (margin * 3)
    val dateColWidth = 11

    fun out(s: String) = print(s)

    fun nl() = println()

    fun YrkesaktivitetUtbetalingsberegningDto.navn() = yaIdToNameMap?.get(yrkesaktivitetId) ?: (yrkesaktivitetId.toString())

    out("".padStart(dateColWidth))
    beregningData.yrkesaktiviteter.forEach { ya ->
        out(ya.navn().center(widthPerYa).with(Style.BOLD_WHITE))
    }
    nl()
    fom.datesUntil(tom.plusDays(1)).forEach { dato ->
        out(
            dato
                .toString()
                .padStart(dateColWidth)
                .with(if (dato.erHelg()) Style.YELLOW else Style.CYAN),
        )
        beregningData.yrkesaktiviteter.forEach { ya ->
            val dag = ya.utbetalingstidslinje.dager.first { it.dato == dato }
            out(dag.økonomi.tilAnsi(margin = margin))
        }
        nl()
    }
}

fun ØkonomiDto.tilAnsi(margin: Int = 1): String {
    val arb = (arbeidsgiverbeløp ?: 0).toInt().toString() // + ",-"
    val pers = (personbeløp ?: 0).toInt().toString() // + ",-"
    val grad = (grad * 100).toInt().toString() + "%"
    val totalGrad = (totalGrad * 100).toInt().toString() + "%"

    return pers.padStart(margin + 4).with(Style.BRIGHT_YELLOW) +
        arb.padStart(margin + 4).with(Style.GREEN) +
        grad.padStart(margin + 4).with(Style.CYAN)
}

private fun BeregningResponseDto.fomTom() =
    beregningData.yrkesaktiviteter.map { it.utbetalingstidslinje }.flatMap { it.dager.map { it.dato } }.toSet().let {
        it.min() to it.max()
    }

private fun String.center(width: Int): String {
    val base =
        this
            .trim()
            .padEnd(width)
            .substring(0, width - 1)
            .trim()
    val spaceLen = width - base.length
    val leftPad = "".padStart(spaceLen.div(2))
    val rightPad = "".padStart(spaceLen.div(2) + spaceLen.mod(2))
    return leftPad + base + rightPad
}
