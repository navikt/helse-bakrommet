package no.nav.helse.bakrommet.behandling.inntekter

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.*
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.fastsettInntektData
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.sammenlikningsgrunnlag
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.sykepenger.Periode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.InntektsmeldingProvider
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider
import kotlin.math.abs

class InntektService(
    val inntektsmeldingProvider: InntektsmeldingProvider,
    val pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
    val inntekterProvider: InntekterProvider,
) {
    fun oppdaterInntekt(
        db: AlleDaoer,
        yrkesaktivitetsperiode: Yrkesaktivitetsperiode,
        behandling: Behandling,
        request: InntektRequest,
        saksbehandler: BrukerOgToken,
    ) {
        db.apply {
            val sykepengegrunnlagId = behandling.sykepengegrunnlagId
            if (sykepengegrunnlagId != null) {
                val spg = sykepengegrunnlagDao.finnSykepengegrunnlag(sykepengegrunnlagId.value)!!
                if (spg.opprettetForBehandling != behandling.id.value) {
                    throw InputValideringException("Gjeldende sykepengegrunnlag er fastsatt på en tidligere saksbehandlingsperiode")
                }
            }

            yrkesaktivitetsperiode.nyInntektRequest(request)

            val inntektData =
                fastsettInntektData(
                    request = request,
                    yrkesaktivitetsperiode = yrkesaktivitetsperiode,
                    periode = behandling,
                    saksbehandler = saksbehandler,
                    inntektsmeldingProvider = inntektsmeldingProvider,
                    inntekterProvider = inntekterProvider,
                    pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
                )

            if (inntektData is InntektData.ArbeidstakerInntektsmelding) {
                val inntektsmeldingJson =
                    this
                        .lastInntektsmeldingDokument(
                            periode = behandling,
                            inntektsmeldingId = inntektData.inntektsmeldingId,
                            inntektsmeldingProvider = inntektsmeldingProvider,
                            saksbehandler = saksbehandler,
                        ).somInntektsmelding()

                val perioder =
                    inntektsmeldingJson.somInntektsmeldingObjekt().arbeidsgiverperioder.map {
                        Periode(
                            it.fom,
                            it.tom,
                        )
                    }
                yrkesaktivitetsperiode.leggTilArbeidsgiverperiode(perioder)
            }

            yrkesaktivitetsperiode.leggTilInntektData(inntektData)
            yrkesaktivitetsperiodeRepository.lagre(yrkesaktivitetsperiode)
            beregnSykepengegrunnlagOgUtbetaling(behandling, saksbehandler.bruker)?.let { rec ->
                requireNotNull(rec.sykepengegrunnlag)

                val yrkesaktiviteter = yrkesaktivitetsperiodeRepository.finn(behandling.id)
                if (yrkesaktiviteter.skalBeregneSammenlikningsgrunnlag()) {
                    if (rec.sammenlikningsgrunnlag == null) {
                        val dokument =
                            lastAInntektSammenlikningsgrunnlag(behandling, inntekterProvider, saksbehandler)
                        val sammenlikningsgrunnlag =
                            dokument.somAInntektSammenlikningsgrunnlag().sammenlikningsgrunnlag()

                        val avvikProsent =
                            if (sammenlikningsgrunnlag.beløp == 0.0) {
                                100.0
                            } else {
                                (
                                    abs(rec.sykepengegrunnlag.beregningsgrunnlag.beløp - sammenlikningsgrunnlag.beløp) /
                                        sammenlikningsgrunnlag.beløp
                                ) * 100.0
                            }

                        sykepengegrunnlagDao.oppdaterSammenlikningsgrunnlag(
                            sykepengegrunnlagId = rec.id,
                            sammenlikningsgrunnlag =
                                Sammenlikningsgrunnlag(
                                    totaltSammenlikningsgrunnlag = sammenlikningsgrunnlag,
                                    avvikProsent = avvikProsent,
                                    avvikMotInntektsgrunnlag = rec.sykepengegrunnlag.beregningsgrunnlag,
                                    basertPåDokumentId = dokument.id,
                                ),
                        )
                    }
                }
            }
        }
    }
}

private fun List<Yrkesaktivitetsperiode>.skalBeregneSammenlikningsgrunnlag(): Boolean =
    this.any {
        when (it.kategorisering) {
            is YrkesaktivitetKategorisering.Arbeidstaker -> true
            is YrkesaktivitetKategorisering.Frilanser -> true
            else -> false
        }
    }
