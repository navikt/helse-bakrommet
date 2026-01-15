package no.nav.helse.bakrommet.infrastruktur.provider

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.auth.AccessToken

interface PensjonsgivendeInntektProvider {
    suspend fun hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(
        fnr: String,
        senesteÅrTom: Int,
        antallÅrBakover: Int,
        saksbehandlerToken: AccessToken,
    ): List<PensjonsgivendeInntektÅrMedSporing>
}

fun PensjonsgivendeInntektÅr.inntektsaar() = this["inntektsaar"]!!.asText().toInt()

typealias PensjonsgivendeInntektÅr = JsonNode
typealias PensjonsgivendeInntektÅrMedSporing = Pair<PensjonsgivendeInntektÅr, Kildespor>

fun PensjonsgivendeInntektÅrMedSporing.data() = this.first

fun PensjonsgivendeInntektÅrMedSporing.sporing() = this.second
