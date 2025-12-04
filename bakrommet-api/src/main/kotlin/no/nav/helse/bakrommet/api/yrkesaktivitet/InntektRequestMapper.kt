package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.inntekter.*

fun InntektRequest.tilInntektRequestDto(): InntektRequestDto =
    when (this) {
        is InntektRequest.Arbeidstaker ->
            InntektRequestDto.Arbeidstaker(
                data = data.tilArbeidstakerInntektRequestDto(),
            )

        is InntektRequest.SelvstendigNæringsdrivende ->
            InntektRequestDto.SelvstendigNæringsdrivende(
                data = data.tilPensjonsgivendeInntektRequestDto(),
            )

        is InntektRequest.Inaktiv ->
            InntektRequestDto.Inaktiv(
                data = data.tilPensjonsgivendeInntektRequestDto(),
            )

        is InntektRequest.Frilanser ->
            InntektRequestDto.Frilanser(
                data = data.tilFrilanserInntektRequestDto(),
            )

        is InntektRequest.Arbeidsledig ->
            InntektRequestDto.Arbeidsledig(
                data = data.tilArbeidsledigInntektRequestDto(),
            )
    }

fun ArbeidstakerInntektRequest.tilArbeidstakerInntektRequestDto(): ArbeidstakerInntektRequestDto =
    when (this) {
        is ArbeidstakerInntektRequest.Inntektsmelding ->
            ArbeidstakerInntektRequestDto.Inntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.tilRefusjonsperiodeDto() },
            )

        is ArbeidstakerInntektRequest.Ainntekt ->
            ArbeidstakerInntektRequestDto.Ainntekt(
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.tilRefusjonsperiodeDto() },
            )

        is ArbeidstakerInntektRequest.Skjønnsfastsatt ->
            ArbeidstakerInntektRequestDto.Skjønnsfastsatt(
                årsinntekt = årsinntekt.beløp.toDouble(),
                årsak = årsak.tilArbeidstakerSkjønnsfastsettelseÅrsakDto(),
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.tilRefusjonsperiodeDto() },
            )
    }

fun PensjonsgivendeInntektRequest.tilPensjonsgivendeInntektRequestDto(): PensjonsgivendeInntektRequestDto =
    when (this) {
        is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt ->
            PensjonsgivendeInntektRequestDto.PensjonsgivendeInntekt(
                begrunnelse = begrunnelse,
            )

        is PensjonsgivendeInntektRequest.Skjønnsfastsatt ->
            PensjonsgivendeInntektRequestDto.Skjønnsfastsatt(
                årsinntekt = årsinntekt.beløp.toDouble(),
                årsak = årsak.tilPensjonsgivendeSkjønnsfastsettelseÅrsakDto(),
                begrunnelse = begrunnelse,
            )
    }

fun FrilanserInntektRequest.tilFrilanserInntektRequestDto(): FrilanserInntektRequestDto =
    when (this) {
        is FrilanserInntektRequest.Ainntekt ->
            FrilanserInntektRequestDto.Ainntekt(
                begrunnelse = begrunnelse,
            )

        is FrilanserInntektRequest.Skjønnsfastsatt ->
            FrilanserInntektRequestDto.Skjønnsfastsatt(
                årsinntekt = årsinntekt.beløp.toDouble(),
                årsak = årsak.tilFrilanserSkjønnsfastsettelseÅrsakDto(),
                begrunnelse = begrunnelse,
            )
    }

fun ArbeidsledigInntektRequest.tilArbeidsledigInntektRequestDto(): ArbeidsledigInntektRequestDto =
    when (this) {
        is ArbeidsledigInntektRequest.Dagpenger ->
            ArbeidsledigInntektRequestDto.Dagpenger(
                dagbeløp = dagbeløp.beløp.toInt(),
                begrunnelse = begrunnelse,
            )

        is ArbeidsledigInntektRequest.Ventelønn ->
            ArbeidsledigInntektRequestDto.Ventelønn(
                årsinntekt = årsinntekt.beløp.toDouble(),
                begrunnelse = begrunnelse,
            )

        is ArbeidsledigInntektRequest.Vartpenger ->
            ArbeidsledigInntektRequestDto.Vartpenger(
                årsinntekt = årsinntekt.beløp.toDouble(),
                begrunnelse = begrunnelse,
            )
    }

fun ArbeidstakerSkjønnsfastsettelseÅrsak.tilArbeidstakerSkjønnsfastsettelseÅrsakDto(): ArbeidstakerSkjønnsfastsettelseÅrsakDto =
    when (this) {
        ArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> ArbeidstakerSkjønnsfastsettelseÅrsakDto.AVVIK_25_PROSENT
        ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> ArbeidstakerSkjønnsfastsettelseÅrsakDto.MANGELFULL_RAPPORTERING
        ArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET -> ArbeidstakerSkjønnsfastsettelseÅrsakDto.TIDSAVGRENSET
    }

fun PensjonsgivendeSkjønnsfastsettelseÅrsak.tilPensjonsgivendeSkjønnsfastsettelseÅrsakDto(): PensjonsgivendeSkjønnsfastsettelseÅrsakDto =
    when (this) {
        PensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING -> PensjonsgivendeSkjønnsfastsettelseÅrsakDto.AVVIK_25_PROSENT_VARIG_ENDRING
        PensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV -> PensjonsgivendeSkjønnsfastsettelseÅrsakDto.SISTE_TRE_YRKESAKTIV
    }

fun FrilanserSkjønnsfastsettelseÅrsak.tilFrilanserSkjønnsfastsettelseÅrsakDto(): FrilanserSkjønnsfastsettelseÅrsakDto =
    when (this) {
        FrilanserSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> FrilanserSkjønnsfastsettelseÅrsakDto.AVVIK_25_PROSENT
        FrilanserSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> FrilanserSkjønnsfastsettelseÅrsakDto.MANGELFULL_RAPPORTERING
    }
