package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Inntektskategori
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

interface InntektServiceDaoer : Beregningsdaoer {
    override val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    override val yrkesaktivitetDao: YrkesaktivitetDao
    override val sykepengegrunnlagDao: SykepengegrunnlagDao
    override val beregningDao: UtbetalingsberegningDao
    override val personDao: PersonDao
}

class InntektService(
    daoer: InntektServiceDaoer,
    val inntektsmeldingClient: InntektsmeldingClient,
    sessionFactory: TransactionalSessionFactory<InntektServiceDaoer>,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    fun oppdaterInntekt(
        ref: YrkesaktivitetReferanse,
        request: InntektRequest,
        saksbehandler: BrukerOgToken,
    ) {
        db.transactional {
            val yrkesaktivitet =
                saksbehandlingsperiodeDao
                    .hentPeriode(
                        ref = ref.saksbehandlingsperiodeReferanse,
                        krav = saksbehandler.bruker.erSaksbehandlerPåSaken(),
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
                        when (request.data) {
                            is ArbeidstakerInntektRequest.Skjønnsfastsatt -> {
                                yrkesaktivitetDao.oppdaterRefusjonsdata(yrkesaktivitet, request.data.refusjon)
                                InntektData.ArbeidstakerSkjønnsfastsatt(
                                    omregnetÅrsinntekt = Inntekt.gjenopprett(request.data.årsinntekt).dto().årlig,
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
                                val inntektsmelding =
                                    runBlocking {
                                        inntektsmeldingClient.hentInntektsmelding(
                                            inntektsmeldingId = request.data.inntektsmeldingId,
                                            saksbehandlerToken = saksbehandler.token,
                                        )
                                    }
                                // TODO valider at fnr og arbeidsgiver stemmer med yrkesaktivitet og person
                                // TODO lagre som dokument
                                InntektData.ArbeidstakerInntektsmelding(
                                    omregnetÅrsinntekt =
                                        InntektbeløpDto
                                            .MånedligDouble(
                                                inntektsmelding.get("beregnetInntekt").asDouble(),
                                            ).tilInntekt()
                                            .dto()
                                            .årlig,
                                    inntektsmeldingId = request.data.inntektsmeldingId,
                                    inntektsmelding = inntektsmelding,
                                    sporing = "ARB_SPG_HOVEDREGEL",
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
                                    beregnetPensjonsgivendeInntekt = InntektbeløpDto.Årlig(800000.0),
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
                                InntektData.InaktivPensjonsgivende(
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
                            is ArbeidsledigInntektRequest.Dagpenger -> {
                                InntektData.Arbeidsledig(
                                    omregnetÅrsinntekt = Inntekt.gjenopprett(request.data.dagbeløp).dto().årlig,
                                )
                            }

                            is ArbeidsledigInntektRequest.Vartpenger ->
                                InntektData.Arbeidsledig(
                                    omregnetÅrsinntekt = Inntekt.gjenopprett(request.data.årsinntekt).dto().årlig,
                                )

                            is ArbeidsledigInntektRequest.Ventelønn ->
                                InntektData.Arbeidsledig(
                                    omregnetÅrsinntekt = Inntekt.gjenopprett(request.data.årsinntekt).dto().årlig,
                                )
                        }
                    }
                }

            yrkesaktivitetDao.oppdaterInntektData(yrkesaktivitet, inntektData)
            beregnSykepengegrunnlagOgUtbetaling(ref.saksbehandlingsperiodeReferanse, saksbehandler.bruker)
        }
    }
}
