package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.FrilanserInntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.FrilanserSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektRequest
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.tilAInntektResponse
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal fun InntektRequest.Frilanser.frilanserFastsettelse(
    behandling: Behandling,
    saksbehandler: BrukerOgToken,
    inntekterProvider: InntekterProvider,
    daoer: AlleDaoer,
): InntektData =
    when (val data = data) {
        is FrilanserInntektRequest.Ainntekt -> {
            val ainntektBeregningsgrunnlag =
                daoer
                    .lastAInntektBeregningsgrunnlag(
                        behandling = behandling,
                        inntekterProvider = inntekterProvider,
                        saksbehandler = saksbehandler,
                    ).somAInntektBeregningsgrunnlag()

            val inntektResponse = ainntektBeregningsgrunnlag.first.tilAInntektResponse()
            val fom = ainntektBeregningsgrunnlag.second.fom
            val tom = ainntektBeregningsgrunnlag.second.tom

            val månederOgInntekt =
                monthsBetween(fom, tom).associateWith { Inntekt.INGEN }.toMutableMap()

            inntektResponse.data.forEach { måned ->
                måned.inntektListe.forEach { inntekt ->
                    månederOgInntekt[måned.maaned] =
                        månederOgInntekt.getValue(måned.maaned) +
                        Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(inntekt.beloep.toDouble()))
                }
            }

            val månedligSnitt =
                månederOgInntekt.values.summer().div(månederOgInntekt.size.toDouble())

            InntektData.FrilanserAinntekt(
                omregnetÅrsinntekt = månedligSnitt,
                kildedata = månederOgInntekt,
            )
        }

        is FrilanserInntektRequest.Skjønnsfastsatt -> {
            val sporing =
                when (data.årsak) {
                    FrilanserSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT -> BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
                    FrilanserSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING -> BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
                }

            InntektData.FrilanserSkjønnsfastsatt(
                omregnetÅrsinntekt = data.årsinntekt,
                sporing = sporing,
            )
        }
    }
