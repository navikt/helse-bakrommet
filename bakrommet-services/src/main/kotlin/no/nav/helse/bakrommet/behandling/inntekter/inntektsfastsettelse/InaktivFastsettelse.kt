package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
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
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider

internal fun InntektRequest.Inaktiv.inaktivFastsettelse(
    periode: BehandlingDbRecord,
    saksbehandler: BrukerOgToken,
    pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
    daoer: DokumentInnhentingDaoer,
): InntektData {
    fun hentPensjonsgivende(): List<HentPensjonsgivendeInntektResponse> =
        daoer
            .lastSigrunDokument(
                periode = periode,
                saksbehandlerToken = saksbehandler.token,
                pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
            ).somPensjonsgivendeInntekt()

    return when (data) {
        is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
            val pensjonsgivendeInntekt = hentPensjonsgivende()

            if (pensjonsgivendeInntekt.kanBeregnesEtter835()) {
                val beregnet =
                    pensjonsgivendeInntekt.tilBeregnetPensjonsgivendeInntekt(periode.skjæringstidspunkt)
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
                    PensjonsgivendeSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT_VARIG_ENDRING -> BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
                    PensjonsgivendeSkjønnsfastsettelseÅrsak.SISTE_TRE_YRKESAKTIV -> BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
                }

            InntektData.InaktivSkjønnsfastsatt(
                omregnetÅrsinntekt = data.årsinntekt,
                sporing = sporing,
            )
        }
    }
}
