package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.AinntektPeriodeNøkkel
import no.nav.helse.bakrommet.infrastruktur.provider.Inntektoppslag
import no.nav.helse.bakrommet.infrastruktur.provider.tilAInntektResponse
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

fun Pair<Inntektoppslag, AinntektPeriodeNøkkel>.omregnetÅrsinntekt(
    orgnummer: String,
): Pair<InntektbeløpDto.Årlig, Map<java.time.YearMonth, InntektbeløpDto.MånedligDouble>> {
    val inntektResponse = first.tilAInntektResponse()
    val fom = second.fom
    val tom = second.tom

    val månederOgInntekt = monthsBetween(fom, tom).associateWith { Inntekt.INGEN }.toMutableMap()
    require(månederOgInntekt.size == 3)

    if (inntektResponse.data.any { !månederOgInntekt.contains(it.maaned) }) {
        throw IllegalStateException("Inntektsdata inneholder måneder utenfor forventet intervall: $fom - $tom")
    }

    inntektResponse.data
        .filter { it.underenhet == orgnummer }
        .forEach {
            it.inntektListe.forEach { inntekt ->
                månederOgInntekt[it.maaned] =
                    månederOgInntekt.getValue(it.maaned) + Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(inntekt.beloep.toDouble()))
            }
        }

    val månedligSnittInntekt = månederOgInntekt.values.summer().div(månederOgInntekt.size.toDouble())
    val månederOgInntektDto = månederOgInntekt.mapValues { it.value.dto().månedligDouble }

    return Pair(månedligSnittInntekt.dto().årlig, månederOgInntektDto)
}

fun Pair<Inntektoppslag, AinntektPeriodeNøkkel>.sammenlikningsgrunnlag(): InntektbeløpDto.Årlig {
    val inntektResponse = first.tilAInntektResponse()
    val fom = second.fom
    val tom = second.tom

    val månederOgInntekt = monthsBetween(fom, tom).associateWith { Inntekt.INGEN }.toMutableMap()
    require(månederOgInntekt.size == 12)

    if (inntektResponse.data.any { !månederOgInntekt.contains(it.maaned) }) {
        throw IllegalStateException("Inntektsdata inneholder måneder utenfor forventet intervall: $fom - $tom")
    }

    val inntektSiste12Måneder =
        inntektResponse.data.flatMap { it.inntektListe.map { inntekt -> inntekt.beloep.toDouble() } }.sum()

    return InntektbeløpDto.Årlig(inntektSiste12Måneder)
}
