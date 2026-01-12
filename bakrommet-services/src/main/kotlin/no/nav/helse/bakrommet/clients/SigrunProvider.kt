package no.nav.helse.bakrommet.clients

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.util.Kildespor

typealias PensjonsgivendeInntektÅr = JsonNode
typealias PensjonsgivendeInntektÅrMedSporing = Pair<PensjonsgivendeInntektÅr, Kildespor>

interface SigrunProvider {
    suspend fun hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(
        fnr: String,
        senesteÅrTom: Int,
        antallÅrBakover: Int,
        saksbehandlerToken: SpilleromBearerToken,
    ): List<PensjonsgivendeInntektÅrMedSporing>

    suspend fun hentPensjonsgivendeInntekt(
        fnr: String,
        inntektsAar: Int,
        saksbehandlerToken: SpilleromBearerToken,
    ): PensjonsgivendeInntektÅr

    suspend fun hentPensjonsgivendeInntektMedSporing(
        fnr: String,
        inntektsAar: Int,
        saksbehandlerToken: SpilleromBearerToken,
    ): Pair<PensjonsgivendeInntektÅr, Kildespor>
}

object SigrunConstants {
    const val INNTEKTSAAR_MIN = 2017
    val INNTEKTSAAR_MAX: Int
        get() = java.time.LocalDate.now().year
    const val INNTEKTSAAR_MAX_COUNT = 10
}
