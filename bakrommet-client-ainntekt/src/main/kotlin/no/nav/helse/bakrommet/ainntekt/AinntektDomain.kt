package no.nav.helse.bakrommet.ainntekt

import java.math.BigDecimal
import java.time.YearMonth

data class InntektApiUt(
    val data: List<Inntektsinformasjon> = emptyList(),
)

data class Inntektsinformasjon(
    val maaned: YearMonth,
    val underenhet: String,
    val opplysningspliktig: String,
    val inntektListe: List<Inntekt> = emptyList(),
)

data class Inntekt(
    val type: String,
    val beloep: BigDecimal,
)
