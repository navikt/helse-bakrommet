package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.DokumentInnhentingDaoer
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastSigrunDokument
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somPensjonsgivendeInntekt
import no.nav.helse.bakrommet.behandling.inntekter.HentPensjonsgivendeInntektResponse
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.PensjonsgivendeInntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.PensjonsgivendeSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.behandling.inntekter.kanBeregnesEtter835
import no.nav.helse.bakrommet.behandling.inntekter.tilBeregnetPensjonsgivendeInntekt
import no.nav.helse.bakrommet.sigrun.SigrunClient

internal fun InntektRequest.SelvstendigNæringsdrivende.selvstendigFastsettelse(
    periode: Behandling,
    saksbehandler: BrukerOgToken,
    sigrunClient: SigrunClient,
    daoer: DokumentInnhentingDaoer,
): InntektData {
    fun hentPensjonsgivende(): List<HentPensjonsgivendeInntektResponse> =
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
                InntektData.SelvstendigNæringsdrivendePensjonsgivende(
                    omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                    pensjonsgivendeInntekt = beregnet,
                )
            } else {
                throw RuntimeException("Kunne ikke beregne inntekt, todo lag spesifikk error")
            }
        }

        is PensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
            val sporing =
                when (data.årsak) {
                    PensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING -> BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
                    PensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV -> BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
                }
            InntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt(
                omregnetÅrsinntekt = data.årsinntekt,
                sporing = sporing,
            )
        }
    }
}
