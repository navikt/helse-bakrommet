package no.nav.helse.bakrommet.infrastruktur.provider

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.auth.AccessToken
import java.time.LocalDate

interface InntektsmeldingProvider {
    suspend fun hentInntektsmeldinger(
        fnr: String,
        fom: LocalDate?,
        tom: LocalDate?,
        saksbehandlerToken: AccessToken,
    ): JsonNode

    suspend fun hentInntektsmeldingMedSporing(
        inntektsmeldingId: String,
        saksbehandlerToken: AccessToken,
    ): Pair<JsonNode, Kildespor>
}
