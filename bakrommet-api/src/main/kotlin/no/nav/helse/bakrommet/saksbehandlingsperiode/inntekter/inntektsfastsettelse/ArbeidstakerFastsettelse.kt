package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.BeregningskoderSykepengrunnlag
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.DokumentInnhentingDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.lastAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.lastInntektsmeldingDokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.somAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.somInntektsmelding
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

internal fun InntektRequest.Arbeidstaker.arbeidstakerFastsettelse(
    yrkesaktivitet: Yrkesaktivitet,
    periode: Saksbehandlingsperiode,
    saksbehandler: BrukerOgToken,
    yrkesaktivitetDao: YrkesaktivitetDao,
    inntektsmeldingClient: InntektsmeldingClient,
    aInntektClient: AInntektClient,
    daoer: DokumentInnhentingDaoer,
): InntektData {
    yrkesaktivitetDao.oppdaterRefusjonsdata(yrkesaktivitet, data.refusjon)

    return when (data) {
        is ArbeidstakerInntektRequest.Skjønnsfastsatt -> {
            val sporing =
                when (data.årsak) {
                    ArbeidstakerSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> BeregningskoderSykepengrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
                    ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> BeregningskoderSykepengrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
                    ArbeidstakerSkjønnsfastsettelseÅrsak.TIDSAVGRENSET -> BeregningskoderSykepengrunnlag.TODO_TRENGER_NY_VERDI
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
                        aInntektClient = aInntektClient,
                        saksbehandler = saksbehandler,
                    ).somAInntektBeregningsgrunnlag()
                    .omregnetÅrsinntekt((yrkesaktivitet.kategorisering as YrkesaktivitetKategorisering.Arbeidstaker).orgnummer)
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

        is ArbeidstakerInntektRequest.ManueltBeregnet -> {
            InntektData.ArbeidstakerManueltBeregnet(
                omregnetÅrsinntekt = data.årsinntekt,
            )
        }
    }
}
