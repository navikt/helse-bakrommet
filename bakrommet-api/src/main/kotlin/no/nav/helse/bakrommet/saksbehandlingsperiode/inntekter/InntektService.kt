package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.*
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.inntektsfastsettelse.fastsettInntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.inntektsfastsettelse.sammenlikningsgrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.sigrun.SigrunClient
import kotlin.math.abs

interface InntektServiceDaoer :
    Beregningsdaoer,
    DokumentInnhentingDaoer {
    override val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    override val yrkesaktivitetDao: YrkesaktivitetDao
    override val sykepengegrunnlagDao: SykepengegrunnlagDao
    override val beregningDao: UtbetalingsberegningDao
    override val personDao: PersonDao
    override val dokumentDao: DokumentDao
}

class InntektService(
    val db: DbDaoer<InntektServiceDaoer>,
    val inntektsmeldingClient: InntektsmeldingClient,
    val sigrunClient: SigrunClient,
    val aInntektClient: AInntektClient,
) {
    suspend fun oppdaterInntekt(
        ref: YrkesaktivitetReferanse,
        request: InntektRequest,
        saksbehandler: BrukerOgToken,
    ) {
        db.transactional {
            val periode =
                saksbehandlingsperiodeDao.hentPeriode(
                    ref = ref.saksbehandlingsperiodeReferanse,
                    krav = saksbehandler.bruker.erSaksbehandlerPåSaken(),
                )
            if (periode.skjæringstidspunkt == null) {
                throw IllegalStateException("Kan ikke oppdatere inntekt før skjæringstidspunkt er satt på saksbehandlingsperiode (id=${periode.id})")
            }
            val yrkesaktivitet =
                yrkesaktivitetDao.hentYrkesaktivitet(ref.yrkesaktivitetUUID)
                    ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")
            require(yrkesaktivitet.saksbehandlingsperiodeId == periode.id) {
                "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
            }

            val feilKategori = { throw IllegalStateException("Feil inntektkategori for oppdatering av inntekt med tyoe ${request.javaClass.name}") }

            when (request) {
                is InntektRequest.Arbeidstaker ->
                    if (yrkesaktivitet.kategorisering !is YrkesaktivitetKategorisering.Arbeidstaker) {
                        feilKategori()
                    }
                is InntektRequest.SelvstendigNæringsdrivende ->
                    if (yrkesaktivitet.kategorisering !is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende) {
                        feilKategori()
                    }
                is InntektRequest.Frilanser ->
                    if (yrkesaktivitet.kategorisering !is YrkesaktivitetKategorisering.Frilanser) {
                        feilKategori()
                    }
                is InntektRequest.Inaktiv ->
                    if (yrkesaktivitet.kategorisering !is YrkesaktivitetKategorisering.Inaktiv) {
                        feilKategori()
                    }
                is InntektRequest.Arbeidsledig ->
                    if (yrkesaktivitet.kategorisering !is YrkesaktivitetKategorisering.Arbeidsledig) {
                        feilKategori()
                    }
            }

            yrkesaktivitetDao.oppdaterInntektrequest(yrkesaktivitet, request)

            val inntektData =
                this.fastsettInntektData(
                    request = request,
                    yrkesaktivitet = yrkesaktivitet,
                    periode = periode,
                    saksbehandler = saksbehandler,
                    yrkesaktivitetDao = yrkesaktivitetDao,
                    inntektsmeldingClient = inntektsmeldingClient,
                    aInntektClient = aInntektClient,
                    sigrunClient = sigrunClient,
                )

            yrkesaktivitetDao.oppdaterInntektData(yrkesaktivitet, inntektData)
            beregnSykepengegrunnlagOgUtbetaling(ref.saksbehandlingsperiodeReferanse, saksbehandler.bruker)?.let { rec ->
                requireNotNull(rec.sykepengegrunnlag)
                val yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktiviteter(periode)
                if (yrkesaktiviteter.skalBeregneSammenlikningsgrunnlag()) {
                    if (rec.sammenlikningsgrunnlag == null) {
                        val dokument =
                            lastAInntektSammenlikningsgrunnlag(periode, aInntektClient, saksbehandler)
                        val sammenlikningsgrunnlag =
                            dokument.somAInntektSammenlikningsgrunnlag().sammenlikningsgrunnlag()

                        val avvikProsent =
                            if (sammenlikningsgrunnlag.beløp == 0.0) {
                                100.0
                            } else {
                                (
                                    abs(rec.sykepengegrunnlag.totaltInntektsgrunnlag.beløp - sammenlikningsgrunnlag.beløp) /
                                        sammenlikningsgrunnlag.beløp
                                ) * 100.0
                            }

                        sykepengegrunnlagDao.oppdaterSammenlikningsgrunnlag(
                            sykepengegrunnlagId = rec.id,
                            sammenlikningsgrunnlag =
                                Sammenlikningsgrunnlag(
                                    totaltSammenlikningsgrunnlag = sammenlikningsgrunnlag,
                                    avvikProsent = avvikProsent,
                                    avvikMotInntektsgrunnlag = rec.sykepengegrunnlag.totaltInntektsgrunnlag,
                                    basertPåDokumentId = dokument.id,
                                ),
                        )
                    }
                }
            }
        }
    }
}

private fun List<Yrkesaktivitet>.skalBeregneSammenlikningsgrunnlag(): Boolean =
    this.any {
        when (it.kategorisering) {
            is YrkesaktivitetKategorisering.Arbeidstaker -> true
            is YrkesaktivitetKategorisering.Frilanser -> true
            else -> false
        }
    }
