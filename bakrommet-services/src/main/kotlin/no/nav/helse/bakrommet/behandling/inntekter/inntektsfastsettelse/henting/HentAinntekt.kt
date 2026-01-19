package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.inntekter.InntektDataOld
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.monthsBetween
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.omregnetÅrsinntekt
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.orgnummer
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.tilAInntektResponse
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

fun AlleDaoer.hentAInntektForYrkesaktivitet(
    yrkesaktivitet: Yrkesaktivitet,
    behandling: Behandling,
    saksbehandler: BrukerOgToken,
    inntekterProvider: InntekterProvider,
): AInntektResponse {
    val kategori = yrkesaktivitet.kategorisering

    return try {
        val ainntektBeregningsgrunnlag =
            lastAInntektBeregningsgrunnlag(
                behandling = behandling,
                inntekterProvider = inntekterProvider,
                saksbehandler = saksbehandler,
            ).somAInntektBeregningsgrunnlag()

        when (kategori) {
            is YrkesaktivitetKategorisering.Arbeidstaker -> {
                val orgnummer = yrkesaktivitet.kategorisering.orgnummer()
                val omregnetÅrsinntekt = ainntektBeregningsgrunnlag.omregnetÅrsinntekt(orgnummer)

                AInntektResponse.Suksess(
                    data =
                        InntektDataOld.ArbeidstakerAinntekt(
                            omregnetÅrsinntekt = omregnetÅrsinntekt.first,
                            kildedata = omregnetÅrsinntekt.second,
                        ),
                )
            }

            is YrkesaktivitetKategorisering.Frilanser -> {
                // For frilanser henter vi all inntekt uten å filtrere på orgnummer
                val inntektResponse = ainntektBeregningsgrunnlag.first.tilAInntektResponse()
                val fom = ainntektBeregningsgrunnlag.second.fom
                val tom = ainntektBeregningsgrunnlag.second.tom

                val månederOgInntekt = monthsBetween(fom, tom).associateWith { Inntekt.INGEN }.toMutableMap()

                inntektResponse.data.forEach { måned ->
                    måned.inntektListe.forEach { inntekt ->
                        månederOgInntekt[måned.maaned] =
                            månederOgInntekt.getValue(måned.maaned) +
                            Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(inntekt.beloep.toDouble()))
                    }
                }

                val månedligSnitt = månederOgInntekt.values.summer().div(månederOgInntekt.size.toDouble())
                val månederOgInntektDto = månederOgInntekt.mapValues { it.value.dto().månedligDouble }

                AInntektResponse.Suksess(
                    data =
                        InntektDataOld.FrilanserAinntekt(
                            omregnetÅrsinntekt = månedligSnitt.dto().årlig,
                            kildedata = månederOgInntektDto,
                        ),
                )
            }

            else -> {
                return AInntektResponse.Feil(
                    feilmelding = "Kan kun hente a-inntekt for arbeidstaker eller frilanser",
                )
            }
        }
    } catch (e: Exception) {
        AInntektResponse.Feil(
            feilmelding = "Kunne ikke hente a-inntekt: ${e.message}",
        )
    }
}

sealed interface AInntektResponse {
    data class Suksess(
        val data: InntektDataOld,
    ) : AInntektResponse

    data class Feil(
        val feilmelding: String,
    ) : AInntektResponse
}
