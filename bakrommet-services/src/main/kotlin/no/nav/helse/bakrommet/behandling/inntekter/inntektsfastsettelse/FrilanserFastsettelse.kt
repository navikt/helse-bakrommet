package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.DokumentInnhentingDaoer
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.inntekter.FrilanserInntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.FrilanserSkjønnsfastsettelseÅrsak.AVVIK_25_PROSENT
import no.nav.helse.bakrommet.behandling.inntekter.FrilanserSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.tilAInntektResponse
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal fun InntektRequest.Frilanser.frilanserFastsettelse(
    periode: BehandlingDbRecord,
    saksbehandler: BrukerOgToken,
    inntekterProvider: InntekterProvider,
    daoer: DokumentInnhentingDaoer,
): InntektData =
    when (data) {
        is FrilanserInntektRequest.Ainntekt -> {
            val ainntektBeregningsgrunnlag =
                daoer
                    .lastAInntektBeregningsgrunnlag(
                        periode = periode,
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
            val månederOgInntektDto = månederOgInntekt.mapValues { it.value.dto().månedligDouble }

            InntektData.FrilanserAinntekt(
                omregnetÅrsinntekt = månedligSnitt.dto().årlig,
                kildedata = månederOgInntektDto,
            )
        }

        is FrilanserInntektRequest.Skjønnsfastsatt -> {
            val sporing =
                when (data.årsak) {
                    AVVIK_25_PROSENT -> FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
                    MANGELFULL_RAPPORTERING -> FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
                }

            InntektData.FrilanserSkjønnsfastsatt(
                omregnetÅrsinntekt = data.årsinntekt,
                sporing = sporing,
            )
        }
    }
