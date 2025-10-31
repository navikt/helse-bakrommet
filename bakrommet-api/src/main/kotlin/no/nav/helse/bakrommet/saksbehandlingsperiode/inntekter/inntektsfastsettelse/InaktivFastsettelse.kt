package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.BeregningskoderSykepengrunnlag
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.DokumentInnhentingDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.lastSigrunDokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.somPensjonsgivendeInntekt
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.HentPensjonsgivendeInntektResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.PensjonsgivendeInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.PensjonsgivendeSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.kanBeregnesEtter835
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.tilBeregnetPensjonsgivendeInntekt
import no.nav.helse.bakrommet.sigrun.SigrunClient

internal suspend fun InntektRequest.Inaktiv.inaktivFastsettelse(
    periode: Saksbehandlingsperiode,
    saksbehandler: BrukerOgToken,
    sigrunClient: SigrunClient,
    daoer: DokumentInnhentingDaoer,
): InntektData {
    suspend fun hentPensjonsgivende(): List<HentPensjonsgivendeInntektResponse> =
        daoer
            .lastSigrunDokument(
                periode = periode,
                saksbehandlerToken = saksbehandler.token,
                sigrunClient = sigrunClient,
            ).somPensjonsgivendeInntekt()

    return when (data) {
        is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
            val pensjonsgivendeInntekt = hentPensjonsgivende()

            if (pensjonsgivendeInntekt.kanBeregnesEtter835()) {
                val beregnet =
                    pensjonsgivendeInntekt.tilBeregnetPensjonsgivendeInntekt(periode.skjæringstidspunkt!!)
                InntektData.InaktivPensjonsgivende(
                    omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                    pensjonsgivendeInntekt = beregnet,
                )
            } else {
                throw RuntimeException("Kan ikke beregne pensjonsgivende inntekt for inaktiv utenfor 835-regelen")
            }
        }

        is PensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
            val sporing =
                when (data.årsak) {
                    PensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING -> BeregningskoderSykepengrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
                    PensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV -> BeregningskoderSykepengrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
                }

            InntektData.InaktivSkjønnsfastsatt(
                omregnetÅrsinntekt = data.årsinntekt,
                sporing = sporing,
            )
        }
    }
}
