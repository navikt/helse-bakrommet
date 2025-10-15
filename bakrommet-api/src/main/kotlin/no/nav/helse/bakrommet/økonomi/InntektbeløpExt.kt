package no.nav.helse.bakrommet.økonomi

import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

fun InntektbeløpDto.Årlig.tilInntekt(): Inntekt = Inntekt.gjenopprett(this)
