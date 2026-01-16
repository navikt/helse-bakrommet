package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.dto.InntektbeløpDto

sealed class InntektRequest {
    data class Arbeidstaker(
        val data: ArbeidstakerInntektRequest,
    ) : InntektRequest()

    data class SelvstendigNæringsdrivende(
        val data: PensjonsgivendeInntektRequest,
    ) : InntektRequest()

    data class Inaktiv(
        val data: PensjonsgivendeInntektRequest,
    ) : InntektRequest()

    data class Frilanser(
        val data: FrilanserInntektRequest,
    ) : InntektRequest()

    data class Arbeidsledig(
        val data: ArbeidsledigInntektRequest,
    ) : InntektRequest()
}

sealed class ArbeidstakerInntektRequest {
    abstract val begrunnelse: String?
    abstract val refusjon: List<Refusjonsperiode>?

    data class Inntektsmelding(
        val inntektsmeldingId: String,
        override val begrunnelse: String? = null,
        override val refusjon: List<Refusjonsperiode>? = null,
    ) : ArbeidstakerInntektRequest()

    data class Ainntekt(
        override val begrunnelse: String,
        override val refusjon: List<Refusjonsperiode>? = null,
    ) : ArbeidstakerInntektRequest()

    data class Skjønnsfastsatt(
        val årsinntekt: InntektbeløpDto.Årlig,
        val årsak: ArbeidstakerSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
        override val refusjon: List<Refusjonsperiode>? = null,
    ) : ArbeidstakerInntektRequest()
}

enum class ArbeidstakerSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT,
    MANGELFULL_RAPPORTERING,
    TIDSAVGRENSET,
}

// SELVSTENDIG_NÆRINGSDRIVENDE / INAKTIV
sealed class PensjonsgivendeInntektRequest {
    abstract val begrunnelse: String

    data class PensjonsgivendeInntekt(
        override val begrunnelse: String,
    ) : PensjonsgivendeInntektRequest()

    data class Skjønnsfastsatt(
        val årsinntekt: InntektbeløpDto.Årlig,
        val årsak: PensjonsgivendeSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
    ) : PensjonsgivendeInntektRequest()
}

enum class PensjonsgivendeSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT_VARIG_ENDRING,
    SISTE_TRE_YRKESAKTIV,
}

// FRILANSER
sealed class FrilanserInntektRequest {
    abstract val begrunnelse: String

    data class Ainntekt(
        override val begrunnelse: String,
    ) : FrilanserInntektRequest()

    data class Skjønnsfastsatt(
        val årsinntekt: InntektbeløpDto.Årlig,
        val årsak: FrilanserSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
    ) : FrilanserInntektRequest()
}

enum class FrilanserSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT,
    MANGELFULL_RAPPORTERING,
}

// ARBEIDSLEDIG
sealed class ArbeidsledigInntektRequest {
    abstract val begrunnelse: String

    data class Dagpenger(
        val dagbeløp: InntektbeløpDto.DagligInt,
        override val begrunnelse: String,
    ) : ArbeidsledigInntektRequest()

    data class Ventelønn(
        val årsinntekt: InntektbeløpDto.Årlig,
        override val begrunnelse: String,
    ) : ArbeidsledigInntektRequest()

    data class Vartpenger(
        val årsinntekt: InntektbeløpDto.Årlig,
        override val begrunnelse: String,
    ) : ArbeidsledigInntektRequest()
}
