package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.inntekter.*
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.dto.InntektbeløpDto

fun InntektRequestDto.tilInntektRequest(): InntektRequest =
    when (this) {
        is InntektRequestDto.Arbeidstaker -> {
            InntektRequest.Arbeidstaker(
                data = data.tilArbeidstakerInntektRequest(),
            )
        }

        is InntektRequestDto.SelvstendigNæringsdrivende -> {
            InntektRequest.SelvstendigNæringsdrivende(
                data = data.tilPensjonsgivendeInntektRequest(),
            )
        }

        is InntektRequestDto.Inaktiv -> {
            InntektRequest.Inaktiv(
                data = data.tilPensjonsgivendeInntektRequest(),
            )
        }

        is InntektRequestDto.Frilanser -> {
            InntektRequest.Frilanser(
                data = data.tilFrilanserInntektRequest(),
            )
        }

        is InntektRequestDto.Arbeidsledig -> {
            InntektRequest.Arbeidsledig(
                data = data.tilArbeidsledigInntektRequest(),
            )
        }
    }

fun ArbeidstakerInntektRequestDto.tilArbeidstakerInntektRequest(): ArbeidstakerInntektRequest =
    when (this) {
        is ArbeidstakerInntektRequestDto.Inntektsmelding -> {
            ArbeidstakerInntektRequest.Inntektsmelding(
                inntektsmeldingId = inntektsmeldingId,
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.tilRefusjonsperiodeOld() },
            )
        }

        is ArbeidstakerInntektRequestDto.Ainntekt -> {
            ArbeidstakerInntektRequest.Ainntekt(
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.tilRefusjonsperiodeOld() },
            )
        }

        is ArbeidstakerInntektRequestDto.Skjønnsfastsatt -> {
            ArbeidstakerInntektRequest.Skjønnsfastsatt(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                årsak = årsak.tilArbeidstakerSkjønnsfastsettelseÅrsak(),
                begrunnelse = begrunnelse,
                refusjon = refusjon?.map { it.tilRefusjonsperiodeOld() },
            )
        }
    }

fun PensjonsgivendeInntektRequestDto.tilPensjonsgivendeInntektRequest(): PensjonsgivendeInntektRequest =
    when (this) {
        is PensjonsgivendeInntektRequestDto.PensjonsgivendeInntekt -> {
            PensjonsgivendeInntektRequest.PensjonsgivendeInntekt(
                begrunnelse = begrunnelse,
            )
        }

        is PensjonsgivendeInntektRequestDto.Skjønnsfastsatt -> {
            PensjonsgivendeInntektRequest.Skjønnsfastsatt(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                årsak = årsak.tilPensjonsgivendeSkjønnsfastsettelseÅrsak(),
                begrunnelse = begrunnelse,
            )
        }
    }

fun FrilanserInntektRequestDto.tilFrilanserInntektRequest(): FrilanserInntektRequest =
    when (this) {
        is FrilanserInntektRequestDto.Ainntekt -> {
            FrilanserInntektRequest.Ainntekt(
                begrunnelse = begrunnelse,
            )
        }

        is FrilanserInntektRequestDto.Skjønnsfastsatt -> {
            FrilanserInntektRequest.Skjønnsfastsatt(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                årsak = årsak.tilFrilanserSkjønnsfastsettelseÅrsak(),
                begrunnelse = begrunnelse,
            )
        }
    }

fun ArbeidsledigInntektRequestDto.tilArbeidsledigInntektRequest(): ArbeidsledigInntektRequest =
    when (this) {
        is ArbeidsledigInntektRequestDto.Dagpenger -> {
            ArbeidsledigInntektRequest.Dagpenger(
                dagbeløp = InntektbeløpDto.DagligInt(dagbeløp),
                begrunnelse = begrunnelse,
            )
        }

        is ArbeidsledigInntektRequestDto.Ventelønn -> {
            ArbeidsledigInntektRequest.Ventelønn(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                begrunnelse = begrunnelse,
            )
        }

        is ArbeidsledigInntektRequestDto.Vartpenger -> {
            ArbeidsledigInntektRequest.Vartpenger(
                årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                begrunnelse = begrunnelse,
            )
        }
    }

fun ArbeidstakerSkjønnsfastsettelseÅrsakDto.tilArbeidstakerSkjønnsfastsettelseÅrsak(): ArbeidstakerSkjønnsfastsettelseÅrsak =
    when (this) {
        ArbeidstakerSkjønnsfastsettelseÅrsakDto.AVVIK_25_PROSENT -> ArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT
        ArbeidstakerSkjønnsfastsettelseÅrsakDto.MANGELFULL_RAPPORTERING -> ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING
        ArbeidstakerSkjønnsfastsettelseÅrsakDto.TIDSAVGRENSET -> ArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET
    }

fun PensjonsgivendeSkjønnsfastsettelseÅrsakDto.tilPensjonsgivendeSkjønnsfastsettelseÅrsak(): PensjonsgivendeSkjønnsfastsettelseÅrsak =
    when (this) {
        PensjonsgivendeSkjønnsfastsettelseÅrsakDto.AVVIK_25_PROSENT_VARIG_ENDRING -> PensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING
        PensjonsgivendeSkjønnsfastsettelseÅrsakDto.SISTE_TRE_YRKESAKTIV -> PensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV
    }

fun FrilanserSkjønnsfastsettelseÅrsakDto.tilFrilanserSkjønnsfastsettelseÅrsak(): FrilanserSkjønnsfastsettelseÅrsak =
    when (this) {
        FrilanserSkjønnsfastsettelseÅrsakDto.AVVIK_25_PROSENT -> FrilanserSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT
        FrilanserSkjønnsfastsettelseÅrsakDto.MANGELFULL_RAPPORTERING -> FrilanserSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING
    }

fun RefusjonsperiodeDto.tilRefusjonsperiodeOld(): Refusjonsperiode =
    Refusjonsperiode(
        fom = fom,
        tom = tom,
        beløp = InntektbeløpDto.MånedligDouble(beløp),
    )
