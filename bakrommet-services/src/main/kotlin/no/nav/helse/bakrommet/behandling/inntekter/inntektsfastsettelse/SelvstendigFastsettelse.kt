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

internal fun InntektRequest.SelvstendigNæringsdrivende.selvstendigFastsettelse(
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
