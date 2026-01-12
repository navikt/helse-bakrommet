package no.nav.helse.bakrommet.infrastruktur.provider

import java.math.BigDecimal
import java.time.YearMonth

data class AInntektResponse(
    val data: List<Inntektsinformasjon> = emptyList(),
)

data class Inntektsinformasjon(
    val maaned: YearMonth,
    val underenhet: String,
    val opplysningspliktig: String = underenhet,
    val inntektListe: List<Inntekt> = emptyList(),
)

data class Inntekt(
    val type: String,
    val beloep: BigDecimal,
)
