package no.nav.helse.bakrommet.clients

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.util.Kildespor
import java.time.LocalDate

interface InntektsmeldingProvider {
    suspend fun hentInntektsmeldinger(
        fnr: String,
        fom: LocalDate?,
        tom: LocalDate?,
        saksbehandlerToken: SpilleromBearerToken,
    ): JsonNode

    suspend fun hentInntektsmeldingMedSporing(
        inntektsmeldingId: String,
        saksbehandlerToken: SpilleromBearerToken,
    ): Pair<JsonNode, Kildespor>
}
