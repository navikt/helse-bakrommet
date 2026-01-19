package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastSigrunDokument
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somPensjonsgivendeInntekt
import no.nav.helse.bakrommet.behandling.inntekter.kanBeregnesEtter835
import no.nav.helse.bakrommet.behandling.inntekter.tilBeregnetPensjonsgivendeInntekt
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider

suspend fun AlleDaoer.hentPensjonsgivendeInntektForYrkesaktivitet(
    yrkesaktivitet: Yrkesaktivitet,
    behandling: Behandling,
    saksbehandler: BrukerOgToken,
    pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
): PensjonsgivendeInntektResponse {
    val kategori = yrkesaktivitet.kategorisering
    if (!(kategori is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende || kategori is YrkesaktivitetKategorisering.Inaktiv)) {
        error("Kan kun hente pensjonsgivende inntekt for selvstendig næringsdrivende eller inaktive yrkesaktiviteter")
    }

    return try {
        val pensjonsgivendeInntekt =
            lastSigrunDokument(
                behandling = behandling,
                saksbehandlerToken = saksbehandler.token,
                pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
            ).somPensjonsgivendeInntekt()

        if (!pensjonsgivendeInntekt.kanBeregnesEtter835()) {
            return PensjonsgivendeInntektResponse.Feil(
                feilmelding = "Mangler pensjonsgivende inntekt for de siste tre årene",
            )
        }

        val beregnet = pensjonsgivendeInntekt.tilBeregnetPensjonsgivendeInntekt(behandling.skjæringstidspunkt)

        when (kategori) {
            is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
                PensjonsgivendeInntektResponse.Suksess(
                    data =
                        InntektData.SelvstendigNæringsdrivendePensjonsgivende(
                            omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                            pensjonsgivendeInntekt = beregnet,
                        ),
                )
            }

            is YrkesaktivitetKategorisering.Inaktiv -> {
                PensjonsgivendeInntektResponse.Suksess(
                    data =
                        InntektData.InaktivPensjonsgivende(
                            omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                            pensjonsgivendeInntekt = beregnet,
                        ),
                )
            }

            else -> {
                throw IllegalStateException("Ugyldig kategori")
            }
        }
    } catch (e: Exception) {
        PensjonsgivendeInntektResponse.Feil(
            feilmelding = "Kunne ikke hente pensjonsgivende inntekt fra Sigrun: ${e.message}",
        )
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
