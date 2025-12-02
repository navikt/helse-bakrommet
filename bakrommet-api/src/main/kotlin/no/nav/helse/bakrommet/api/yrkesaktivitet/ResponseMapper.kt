package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.AInntektResponse
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.PensjonsgivendeInntektResponse

fun PensjonsgivendeInntektResponse.tilPensjonsgivendeInntektResponseDto(): PensjonsgivendeInntektResponseDto =
    when (this) {
        is PensjonsgivendeInntektResponse.Suksess ->
            PensjonsgivendeInntektResponseDto.Suksess(
                data = data.tilInntektDataDto(),
            )

        is PensjonsgivendeInntektResponse.Feil ->
            PensjonsgivendeInntektResponseDto.Feil(
                feilmelding = feilmelding,
            )
    }

fun AInntektResponse.tilAinntektResponseDto(): AinntektResponseDto =
    when (this) {
        is AInntektResponse.Suksess ->
            AinntektResponseDto.Suksess(
                data = data.tilInntektDataDto(),
            )

        is AInntektResponse.Feil ->
            AinntektResponseDto.Feil(
                feilmelding = feilmelding,
            )
    }
