package no.nav.helse.bakrommet.clients

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.util.Kildespor
import java.time.YearMonth

typealias Inntektoppslag = JsonNode

enum class AInntektFilter {
    `8-28`,
    `8-30`,
}

interface AInntektProvider {
    suspend fun hentInntekterFor(
        fnr: String,
        maanedFom: YearMonth,
        maanedTom: YearMonth,
        filter: AInntektFilter,
        saksbehandlerToken: SpilleromBearerToken,
    ): Inntektoppslag

    suspend fun hentInntekterForMedSporing(
        fnr: String,
        maanedFom: YearMonth,
        maanedTom: YearMonth,
        filter: AInntektFilter,
        saksbehandlerToken: SpilleromBearerToken,
    ): Pair<Inntektoppslag, Kildespor>
}
