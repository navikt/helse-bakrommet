package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting

import no.nav.helse.bakrommet.ainntekt.tilInntektApiUt
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.monthsBetween
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.omregnetÅrsinntekt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.orgnummer
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

suspend fun InntektService.hentAInntektForYrkesaktivitet(
    ref: YrkesaktivitetReferanse,
    saksbehandler: BrukerOgToken,
): AInntektResponse {
    return db.transactional {
        val periode =
            behandlingDao.hentPeriode(
                ref = ref.behandlingReferanse,
                krav = saksbehandler.bruker.erSaksbehandlerPåSaken(),
            )

        val yrkesaktivitet =
            yrkesaktivitetDao.hentYrkesaktivitet(ref.yrkesaktivitetUUID)
                ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")

        require(yrkesaktivitet.behandlingId == periode.id) {
            "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
        }

        val kategori = yrkesaktivitet.kategorisering

        try {
            val ainntektBeregningsgrunnlag =
                lastAInntektBeregningsgrunnlag(
                    periode = periode,
                    aInntektClient = aInntektClient,
                    saksbehandler = saksbehandler,
                ).somAInntektBeregningsgrunnlag()

            when (kategori) {
                is YrkesaktivitetKategorisering.Arbeidstaker -> {
                    val orgnummer = yrkesaktivitet.kategorisering.orgnummer()
                    val omregnetÅrsinntekt = ainntektBeregningsgrunnlag.omregnetÅrsinntekt(orgnummer)

                    AInntektResponse.Suksess(
                        data =
                            InntektData.ArbeidstakerAinntekt(
                                omregnetÅrsinntekt = omregnetÅrsinntekt.first,
                                kildedata = omregnetÅrsinntekt.second,
                            ),
                    )
                }

                is YrkesaktivitetKategorisering.Frilanser -> {
                    // For frilanser henter vi all inntekt uten å filtrere på orgnummer
                    val inntektResponse = ainntektBeregningsgrunnlag.first.tilInntektApiUt()
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
                            InntektData.FrilanserAinntekt(
                                omregnetÅrsinntekt = månedligSnitt.dto().årlig,
                                kildedata = månederOgInntektDto,
                            ),
                    )
                }

                else -> return@transactional AInntektResponse.Feil(
                    feilmelding = "Kan kun hente a-inntekt for arbeidstaker eller frilanser",
                )
            }
        } catch (e: Exception) {
            AInntektResponse.Feil(
                feilmelding = "Kunne ikke hente a-inntekt: ${e.message}",
            )
        }
    }
}

sealed interface AInntektResponse {
    data class Suksess(
        val data: InntektData,
    ) : AInntektResponse

    data class Feil(
        val feilmelding: String,
    ) : AInntektResponse
}
