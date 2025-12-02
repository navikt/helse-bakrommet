package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastSigrunDokument
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somPensjonsgivendeInntekt
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.kanBeregnesEtter835
import no.nav.helse.bakrommet.behandling.inntekter.tilBeregnetPensjonsgivendeInntekt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering.Inaktiv
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering.SelvstendigNæringsdrivende
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException

suspend fun InntektService.hentPensjonsgivendeInntektForYrkesaktivitet(
    ref: YrkesaktivitetReferanse,
    saksbehandler: BrukerOgToken,
): PensjonsgivendeInntektResponse {
    return db.transactional {
        val periode =
            behandlingDao.hentPeriode(
                ref = ref.saksbehandlingsperiodeReferanse,
                krav = saksbehandler.bruker.erSaksbehandlerPåSaken(),
            )

        val yrkesaktivitet =
            yrkesaktivitetDao.hentYrkesaktivitet(ref.yrkesaktivitetUUID)
                ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")

        require(yrkesaktivitet.saksbehandlingsperiodeId == periode.id) {
            "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
        }

        val kategori = yrkesaktivitet.kategorisering
        if (kategori !is SelvstendigNæringsdrivende && kategori !is Inaktiv) {
        }

        try {
            val pensjonsgivendeInntekt =
                lastSigrunDokument(
                    periode = periode,
                    saksbehandlerToken = saksbehandler.token,
                    sigrunClient = sigrunClient,
                ).somPensjonsgivendeInntekt()

            if (!pensjonsgivendeInntekt.kanBeregnesEtter835()) {
                return@transactional PensjonsgivendeInntektResponse.Feil(
                    feilmelding = "Mangler pensjonsgivende inntekt for de siste tre årene",
                )
            }

            val beregnet = pensjonsgivendeInntekt.tilBeregnetPensjonsgivendeInntekt(periode.skjæringstidspunkt)

            when (kategori) {
                is SelvstendigNæringsdrivende ->
                    PensjonsgivendeInntektResponse.Suksess(
                        data =
                            InntektData.SelvstendigNæringsdrivendePensjonsgivende(
                                omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                                pensjonsgivendeInntekt = beregnet,
                            ),
                    )

                is Inaktiv ->
                    PensjonsgivendeInntektResponse.Suksess(
                        data =
                            InntektData.InaktivPensjonsgivende(
                                omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                                pensjonsgivendeInntekt = beregnet,
                            ),
                    )

                else -> throw IllegalStateException("Ugyldig kategori")
            }
        } catch (e: Exception) {
            PensjonsgivendeInntektResponse.Feil(
                feilmelding = "Kunne ikke hente pensjonsgivende inntekt fra Sigrun: ${e.message}",
            )
        }
    }
}

sealed interface PensjonsgivendeInntektResponse {
    data class Suksess(
        val data: InntektData,
    ) : PensjonsgivendeInntektResponse

    data class Feil(
        val feilmelding: String,
    ) : PensjonsgivendeInntektResponse
}
