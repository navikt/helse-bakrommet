package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.ArbeidsledigInntektType
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.FrilanserInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.PensjonsgivendeInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetReferanse
import java.util.*

interface InntektServiceDaoer {
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val beregningDao: UtbetalingsberegningDao
    val personDao: PersonDao
}

typealias DagerSomSkalOppdateres = JsonNode

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
            // TODO: Implementer lagring av inntekt request og eksterne data
            // For nå bare logg at vi mottok requesten
            when (request) {
                is InntektRequest.Arbeidstaker ->
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
