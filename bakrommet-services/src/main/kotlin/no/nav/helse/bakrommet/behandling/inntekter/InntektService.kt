package no.nav.helse.bakrommet.behandling.inntekter

import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.*
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.fastsettInntektData
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.sammenlikningsgrunnlag
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.tilYrkesaktivitetDbRecord
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.person.PersonPseudoIdDao
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.dto.PeriodeDto
import kotlin.math.abs

interface InntektServiceDaoer :
    Beregningsdaoer,
    DokumentInnhentingDaoer {
    override val behandlingDao: BehandlingDao
    override val yrkesaktivitetDao: YrkesaktivitetDao
    override val sykepengegrunnlagDao: SykepengegrunnlagDao
    override val beregningDao: UtbetalingsberegningDao
    override val personPseudoIdDao: PersonPseudoIdDao
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
                behandlingDao.hentPeriode(
                    ref = ref.behandlingReferanse,
                    krav = saksbehandler.bruker.erSaksbehandlerPåSaken(),
                )

            if (periode.sykepengegrunnlagId != null) {
                val spg = sykepengegrunnlagDao.finnSykepengegrunnlag(periode.sykepengegrunnlagId)!!
                if (spg.opprettetForBehandling != periode.id) {
                    throw InputValideringException("Gjeldende sykepengegrunnlag er fastsatt på en tidligere saksbehandlingsperiode")
                }
            }

            val yrkesaktivitet =
                yrkesaktivitetDao.hentYrkesaktivitet(ref.yrkesaktivitetUUID)
                    ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")
            require(yrkesaktivitet.saksbehandlingsperiodeId == periode.id) {
                "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
            }

            val feilKategori =
                { throw IllegalStateException("Feil inntektkategori for oppdatering av inntekt med tyoe ${request.javaClass.name}") }

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

            if (inntektData is InntektData.ArbeidstakerInntektsmelding) {
                val inntektsmeldingJson =
                    this
                        .lastInntektsmeldingDokument(
                            periode = periode,
                            inntektsmeldingId = inntektData.inntektsmeldingId,
                            inntektsmeldingClient = inntektsmeldingClient,
                            saksbehandler = saksbehandler,
                        ).somInntektsmelding()

                yrkesaktivitetDao.oppdaterPerioder(
                    yrkesaktivitet.tilYrkesaktivitetDbRecord(),
                    Perioder(Periodetype.ARBEIDSGIVERPERIODE, inntektsmeldingJson.somInntektsmeldingObjekt().arbeidsgiverperioder.map { PeriodeDto(it.fom, it.tom) }),
                )
            }

            yrkesaktivitetDao.oppdaterInntektData(yrkesaktivitet, inntektData)
            beregnSykepengegrunnlagOgUtbetaling(ref.behandlingReferanse, saksbehandler.bruker)?.let { rec ->
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

private fun List<Yrkesaktivitet>.skalBeregneSammenlikningsgrunnlag(): Boolean =
    this.any {
        when (it.kategorisering) {
            is YrkesaktivitetKategorisering.Arbeidstaker -> true
            is YrkesaktivitetKategorisering.Frilanser -> true
            else -> false
        }
    }
