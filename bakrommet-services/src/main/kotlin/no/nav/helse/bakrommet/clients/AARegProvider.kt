package no.nav.helse.bakrommet.clients

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.util.Kildespor

typealias Arbeidsforholdoppslag = JsonNode

interface AARegProvider {
    suspend fun hentArbeidsforholdFor(
        fnr: String,
        saksbehandlerToken: SpilleromBearerToken,
    ): Arbeidsforholdoppslag

    suspend fun hentArbeidsforholdForMedSporing(
        fnr: String,
        saksbehandlerToken: SpilleromBearerToken,
    ): Pair<Arbeidsforholdoppslag, Kildespor>
}
