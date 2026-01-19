package no.nav.helse.bakrommet.infrastruktur.provider

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import java.time.YearMonth

fun Inntektoppslag.tilAInntektResponse(): AInntektResponse = objectMapper.readValue(this.serialisertTilString())

typealias Inntektoppslag = JsonNode

enum class AInntektFilter {
    `8-28`,
    `8-30`,
}

interface InntekterProvider {
    suspend fun hentInntekterForMedSporing(
        fnr: String,
        maanedFom: YearMonth,
        maanedTom: YearMonth,
        filter: AInntektFilter,
        saksbehandlerToken: AccessToken,
    ): Pair<Inntektoppslag, Kildespor>
}
