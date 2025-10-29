package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.ainntekt.tilInntektApiUt
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.DokumentInnhentingDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.lastAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.somAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.FrilanserInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal fun InntektRequest.Frilanser.frilanserFastsettelse(
    periode: Saksbehandlingsperiode,
    saksbehandler: BrukerOgToken,
    aInntektClient: AInntektClient,
    daoer: DokumentInnhentingDaoer,
): InntektData =
    when (data) {
        is FrilanserInntektRequest.Ainntekt -> {
            val ainntektBeregningsgrunnlag =
                daoer
                    .lastAInntektBeregningsgrunnlag(
                        periode = periode,
                        aInntektClient = aInntektClient,
                        saksbehandler = saksbehandler,
                    ).somAInntektBeregningsgrunnlag()

            val inntektResponse = ainntektBeregningsgrunnlag.first.tilInntektApiUt()
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
            InntektData.FrilanserSkjønnsfastsatt(
                omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
            )
        }
    }
