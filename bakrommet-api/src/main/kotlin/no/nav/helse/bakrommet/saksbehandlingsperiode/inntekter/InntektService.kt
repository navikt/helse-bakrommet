package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlagold.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Inntektskategori
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

interface InntektServiceDaoer {
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val beregningDao: UtbetalingsberegningDao
    val personDao: PersonDao
}

class InntektService(
    daoer: InntektServiceDaoer,
    sessionFactory: TransactionalSessionFactory<InntektServiceDaoer>,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    fun oppdaterInntekt(
        ref: YrkesaktivitetReferanse,
        request: InntektRequest,
        saksbehandler: Bruker,
    ) {
        db.transactional {
            val yrkesaktivitet =
                saksbehandlingsperiodeDao
                    .hentPeriode(
                        ref = ref.saksbehandlingsperiodeReferanse,
                        krav = saksbehandler.erSaksbehandlerPåSaken(),
                    ).let { periode ->
                        val yrkesaktivitet =
                            yrkesaktivitetDao.hentYrkesaktivitet(ref.yrkesaktivitetUUID)
                                ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")
                        require(yrkesaktivitet.saksbehandlingsperiodeId == periode.id) {
                            "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
                        }
                        yrkesaktivitet
                    }

            fun validerInntektskategori(forventet: Inntektskategori) {
                if (yrkesaktivitet.kategorisering.inntektskategori != forventet) {
                    throw IllegalStateException("Kan kun oppdatere ${forventet.name} inntekt på yrkesaktivitet med inntektskategori ${forventet.name}")
                }
            }

            when (request) {
                is InntektRequest.Arbeidstaker -> validerInntektskategori(Inntektskategori.ARBEIDSTAKER)
                is InntektRequest.SelvstendigNæringsdrivende -> validerInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
                is InntektRequest.Frilanser -> validerInntektskategori(Inntektskategori.FRILANSER)
                is InntektRequest.Inaktiv -> validerInntektskategori(Inntektskategori.INAKTIV)
                is InntektRequest.Arbeidsledig -> validerInntektskategori(Inntektskategori.ARBEIDSLEDIG)
            }

            yrkesaktivitetDao.oppdaterInntektrequest(yrkesaktivitet, request)

            val inntektData =
                when (request) {
                    is InntektRequest.Arbeidstaker -> {
                        if (yrkesaktivitet.kategorisering.inntektskategori == Inntektskategori.ARBEIDSTAKER) {
                            throw IllegalStateException("Kan kun oppdatere arbeidstaker inntekt på yrkesaktivitet med inntektskategori ARBEIDSTAKER")
                        }

                        when (request.data) {
                            is ArbeidstakerInntektRequest.Skjønnsfastsatt -> {
                                InntektData.ArbeidstakerSkjønnsfastsatt(
                                    omregnetÅrsinntekt = Inntekt.gjenopprett(request.data.månedsbeløp).dto().årlig,
                                    sporing = "SKJØNNSFASTSATT_${request.data.årsak.name} TODO",
                                )
                            }

                            is ArbeidstakerInntektRequest.Ainntekt -> {
                                InntektData.ArbeidstakerAinntekt(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                    sporing = "A-inntekt TODO",
                                )
                            }

                            is ArbeidstakerInntektRequest.Inntektsmelding -> {
                                // TODO: Hent inntektsmelding data
                                InntektData.ArbeidstakerInntektsmelding(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                    inntektsmeldingId = request.data.inntektsmeldingId,
                                )
                            }

                            is ArbeidstakerInntektRequest.ManueltBeregnet -> {
                                InntektData.ArbeidstakerManueltBeregnet(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                )
                            }
                        }
                    }
                    is InntektRequest.SelvstendigNæringsdrivende ->
                        when (request.data) {
                            is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
                                InntektData.SelvstendigNæringsdrivendePensjonsgivende(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                    pensjonsgivendeInntekt = PensjonsgivendeInntekt(emptyList()), // TODO dette skal hentes,
                                )
                            }
                            is PensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
                                InntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                )
                            }
                        }
                    is InntektRequest.Inaktiv ->
                        when (request.data) {
                            is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
                                InntektData.SelvstendigNæringsdrivendePensjonsgivende(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                    pensjonsgivendeInntekt = PensjonsgivendeInntekt(emptyList()), // TODO dette skal hentes,
                                )
                            }
                            is PensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
                                InntektData.InaktivSkjønnsfastsatt(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                )
                            }
                        }
                    is InntektRequest.Frilanser ->
                        when (request.data) {
                            is FrilanserInntektRequest.Ainntekt -> {
                                InntektData.FrilanserAinntekt(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                    sporing = "A-inntekt TODO",
                                )
                            }
                            is FrilanserInntektRequest.Skjønnsfastsatt -> {
                                InntektData.FrilanserSkjønnsfastsatt(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                    sporing = "A-inntekt TODO",
                                )
                            }
                        }
                    is InntektRequest.Arbeidsledig -> {
                        when (request.data) {
                            is ArbeidsledigInntektRequest.Dagpenger -> TODO()
                            is ArbeidsledigInntektRequest.Vartpenger -> TODO()
                            is ArbeidsledigInntektRequest.Ventelønn -> TODO()
                        }
                    }
                }

            yrkesaktivitetDao.oppdaterInntektData(yrkesaktivitet, inntektData)
            // Slett sykepengegrunnlag og utbetalingsberegning når inntekt endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.saksbehandlingsperiodeReferanse.periodeUUID)
            beregningDao.slettBeregning(ref.saksbehandlingsperiodeReferanse.periodeUUID)
        }
    }
}
