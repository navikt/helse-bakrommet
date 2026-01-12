package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.*
import no.nav.helse.bakrommet.behandling.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.ArbeidstakerSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.orgnummer
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

internal fun InntektRequest.Arbeidstaker.arbeidstakerFastsettelse(
    yrkesaktivitet: Yrkesaktivitet,
    periode: BehandlingDbRecord,
    saksbehandler: BrukerOgToken,
    yrkesaktivitetDao: YrkesaktivitetDao,
    inntektsmeldingClient: InntektsmeldingClient,
    inntekterProvider: InntekterProvider,
    daoer: DokumentInnhentingDaoer,
): InntektData {
    yrkesaktivitetDao.oppdaterRefusjon(yrkesaktivitet.id, data.refusjon)

    return when (data) {
        is ArbeidstakerInntektRequest.Skjønnsfastsatt -> {
            val sporing =
                when (data.årsak) {
                    ArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
                    ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
                    ArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO
                }
            InntektData.ArbeidstakerSkjønnsfastsatt(
                omregnetÅrsinntekt = Inntekt.gjenopprett(data.årsinntekt).dto().årlig,
                sporing = sporing,
            )
        }

        is ArbeidstakerInntektRequest.Ainntekt -> {
            val omregnetÅrsinntekt =
                daoer
                    .lastAInntektBeregningsgrunnlag(
                        periode = periode,
                        inntekterProvider = inntekterProvider,
                        saksbehandler = saksbehandler,
                    ).somAInntektBeregningsgrunnlag()
                    .omregnetÅrsinntekt(yrkesaktivitet.kategorisering.orgnummer())
            InntektData.ArbeidstakerAinntekt(
                omregnetÅrsinntekt = omregnetÅrsinntekt.first,
                kildedata = omregnetÅrsinntekt.second,
            )
        }

        is ArbeidstakerInntektRequest.Inntektsmelding -> {
            val inntektsmelding =
                daoer
                    .lastInntektsmeldingDokument(
                        periode = periode,
                        inntektsmeldingId = data.inntektsmeldingId,
                        inntektsmeldingClient = inntektsmeldingClient,
                        saksbehandler = saksbehandler,
                    ).somInntektsmelding()
            InntektData.ArbeidstakerInntektsmelding(
                omregnetÅrsinntekt =
                    InntektbeløpDto
                        .MånedligDouble(
                            inntektsmelding.get("beregnetInntekt").asDouble(),
                        ).tilInntekt()
                        .dto()
                        .årlig,
                inntektsmeldingId = data.inntektsmeldingId,
                inntektsmelding = inntektsmelding,
            )
        }
    }
}
