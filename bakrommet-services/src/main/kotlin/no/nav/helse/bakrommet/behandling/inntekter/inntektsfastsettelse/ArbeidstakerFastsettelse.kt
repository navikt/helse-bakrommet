package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastInntektsmeldingDokument
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somInntektsmelding
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.ArbeidstakerSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.orgnummer
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.InntektsmeldingProvider
import no.nav.helse.bakrommet.serialisertTilString
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

internal fun InntektRequest.Arbeidstaker.arbeidstakerFastsettelse(
    yrkesaktivitet: Yrkesaktivitet,
    behandling: Behandling,
    saksbehandler: BrukerOgToken,
    inntektsmeldingProvider: InntektsmeldingProvider,
    inntekterProvider: InntekterProvider,
    daoer: AlleDaoer,
): InntektData {
    yrkesaktivitet.oppdaterRefusjon(data.refusjon)

    return when (val data = data) {
        is ArbeidstakerInntektRequest.Skjønnsfastsatt -> {
            val sporing =
                when (data.årsak) {
                    ArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
                    ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
                    ArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO
                }
            InntektData.ArbeidstakerSkjønnsfastsatt(
                omregnetÅrsinntekt = data.årsinntekt,
                sporing = sporing,
            )
        }

        is ArbeidstakerInntektRequest.Ainntekt -> {
            val omregnetÅrsinntekt =
                daoer
                    .lastAInntektBeregningsgrunnlag(
                        behandling = behandling,
                        inntekterProvider = inntekterProvider,
                        saksbehandler = saksbehandler,
                    ).somAInntektBeregningsgrunnlag()
                    .omregnetÅrsinntekt(yrkesaktivitet.kategorisering.orgnummer())
            InntektData.ArbeidstakerAinntekt(
                omregnetÅrsinntekt = Inntekt.gjenopprett(omregnetÅrsinntekt.first),
                kildedata =
                    omregnetÅrsinntekt.second.mapValues {
                        Inntekt.gjenopprett(it.value)
                    },
            )
        }

        is ArbeidstakerInntektRequest.Inntektsmelding -> {
            val inntektsmelding =
                daoer
                    .lastInntektsmeldingDokument(
                        periode = behandling,
                        inntektsmeldingId = data.inntektsmeldingId,
                        inntektsmeldingProvider = inntektsmeldingProvider,
                        saksbehandler = saksbehandler,
                    ).somInntektsmelding()
            InntektData.ArbeidstakerInntektsmelding(
                omregnetÅrsinntekt =
                    InntektbeløpDto
                        .MånedligDouble(
                            inntektsmelding.get("beregnetInntekt").asDouble(),
                        ).tilInntekt(),
                inntektsmeldingId = data.inntektsmeldingId,
                inntektsmelding = inntektsmelding.serialisertTilString(),
            )
        }
    }
}
