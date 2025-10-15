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

            when (request) {
                is InntektRequest.Arbeidstaker -> {
                    if (yrkesaktivitet.kategorisering.inntektskategori == Inntektskategori.ARBEIDSTAKER) {
                        throw IllegalStateException("Kan kun oppdatere arbeidstaker inntekt på yrkesaktivitet med inntektskategori ARBEIDSTAKER")
                    }

                    when (request.data) {
                        is ArbeidstakerInntektRequest.Skjønnsfastsatt -> {
                            // Logg skjønnsfastsatt inntekt
                        }

                        is ArbeidstakerInntektRequest.Ainntekt -> {
                            // TODO: Hent A-inntekt data
                        }

                        is ArbeidstakerInntektRequest.Inntektsmelding -> {
                            // TODO: Hent inntektsmelding data
                        }

                        is ArbeidstakerInntektRequest.ManueltBeregnet -> {
                            // Logg manuelt beregnet inntekt
                        }
                    }
                }
                is InntektRequest.SelvstendigNæringsdrivende ->
                    when (request.data) {
                        is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
                            // TODO: Hent pensjonsgivende inntekt data
                        }
                        is PensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
                            // Logg skjønnsfastsatt pensjonsgivende inntekt
                        }
                    }
                is InntektRequest.Inaktiv ->
                    when (request.data) {
                        is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
                            // TODO: Hent pensjonsgivende inntekt data
                        }
                        is PensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
                            // Logg skjønnsfastsatt pensjonsgivende inntekt
                        }
                    }
                is InntektRequest.Frilanser ->
                    when (request.data) {
                        is FrilanserInntektRequest.Ainntekt -> {
                            // TODO: Hent A-inntekt data
                        }
                        is FrilanserInntektRequest.Skjønnsfastsatt -> {
                            // Logg skjønnsfastsatt frilanser inntekt
                        }
                    }
                is InntektRequest.Arbeidsledig -> {
                    when (request.data.type) {
                        ArbeidsledigInntektType.DAGPENGER -> {
                            // Logg dagpenger: månedligBeløp = ${request.data.månedligBeløp}
                        }
                        ArbeidsledigInntektType.VENTELONN -> {
                            // Logg ventelønn: månedligBeløp = ${request.data.månedligBeløp}
                        }
                        ArbeidsledigInntektType.VARTPENGER -> {
                            // Logg vartpenger: månedligBeløp = ${request.data.månedligBeløp}
                        }
                    }
                }
            }

            // Slett sykepengegrunnlag og utbetalingsberegning når inntekt endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.saksbehandlingsperiodeReferanse.periodeUUID)
            beregningDao.slettBeregning(ref.saksbehandlingsperiodeReferanse.periodeUUID)
        }
    }
}
