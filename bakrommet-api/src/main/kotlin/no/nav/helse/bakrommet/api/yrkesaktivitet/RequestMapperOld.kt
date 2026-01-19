package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.ArbeidsledigInntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.ArbeidstakerSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.FrilanserInntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.FrilanserSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.PensjonsgivendeInntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.PensjonsgivendeSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

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
                årsinntekt = årsinntekt.somÅrligInntekt(),
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
                årsinntekt = årsinntekt.somÅrligInntekt(),
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
                årsinntekt = årsinntekt.somÅrligInntekt(),
                årsak = årsak.tilFrilanserSkjønnsfastsettelseÅrsak(),
                begrunnelse = begrunnelse,
            )
        }
    }

fun ArbeidsledigInntektRequestDto.tilArbeidsledigInntektRequest(): ArbeidsledigInntektRequest =
    when (this) {
        is ArbeidsledigInntektRequestDto.Dagpenger -> {
            ArbeidsledigInntektRequest.Dagpenger(
                dagbeløp = Inntekt.gjenopprett(InntektbeløpDto.DagligInt(dagbeløp)),
                begrunnelse = begrunnelse,
            )
        }

        is ArbeidsledigInntektRequestDto.Ventelønn -> {
            ArbeidsledigInntektRequest.Ventelønn(
                årsinntekt = årsinntekt.somÅrligInntekt(),
                begrunnelse = begrunnelse,
            )
        }

        is ArbeidsledigInntektRequestDto.Vartpenger -> {
            ArbeidsledigInntektRequest.Vartpenger(
                årsinntekt = årsinntekt.somÅrligInntekt(),
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
        beløp = beløp.somMånedligInntekt(),
    )

fun Double.somMånedligInntekt(): Inntekt = Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(this))

fun Double.somÅrligInntekt(): Inntekt = Inntekt.gjenopprett(InntektbeløpDto.Årlig(this))
