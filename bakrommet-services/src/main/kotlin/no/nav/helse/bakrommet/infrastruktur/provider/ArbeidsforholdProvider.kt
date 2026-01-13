package no.nav.helse.bakrommet.infrastruktur.provider

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.util.Kildespor

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
