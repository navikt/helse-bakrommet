package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastSigrunDokument
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somPensjonsgivendeInntekt
import no.nav.helse.bakrommet.behandling.inntekter.HentPensjonsgivendeInntektResponse
import no.nav.helse.bakrommet.behandling.inntekter.kanBeregnesEtter835
import no.nav.helse.bakrommet.behandling.inntekter.tilBeregnetPensjonsgivendeInntekt
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.PensjonsgivendeInntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.PensjonsgivendeSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider

internal fun InntektRequest.Inaktiv.inaktivFastsettelse(
    behandling: Behandling,
    saksbehandler: BrukerOgToken,
    pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
    daoer: AlleDaoer,
): InntektData {
    fun hentPensjonsgivende(): List<HentPensjonsgivendeInntektResponse> =
        daoer
            .lastSigrunDokument(
                behandling = behandling,
                saksbehandlerToken = saksbehandler.token,
                pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
            ).somPensjonsgivendeInntekt()

    return when (val data = data) {
        is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
            val pensjonsgivendeInntekt = hentPensjonsgivende()

            if (pensjonsgivendeInntekt.kanBeregnesEtter835()) {
                val beregnet =
                    pensjonsgivendeInntekt.tilBeregnetPensjonsgivendeInntekt(behandling.skjæringstidspunkt)
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
