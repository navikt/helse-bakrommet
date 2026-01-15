package no.nav.helse.bakrommet.infrastruktur.provider

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.auth.AccessToken

typealias Arbeidsforholdoppslag = JsonNode

interface ArbeidsforholdProvider {
    suspend fun hentArbeidsforholdFor(
        fnr: String,
        saksbehandlerToken: AccessToken,
    ): Arbeidsforholdoppslag

    suspend fun hentArbeidsforholdForMedSporing(
        fnr: String,
        saksbehandlerToken: AccessToken,
    ): Pair<Arbeidsforholdoppslag, Kildespor>
}
