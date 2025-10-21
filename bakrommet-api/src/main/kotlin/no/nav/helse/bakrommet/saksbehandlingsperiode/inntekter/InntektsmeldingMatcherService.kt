package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.plugins.BadRequestException
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.SessionDaoerFelles
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.tilYrkesaktivitet

interface InntektsmeldingMatcherDaoer {
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val beregningDao: UtbetalingsberegningDao
    val personDao: PersonDao
}

class InntektsmeldingMatcherService(
    daoer: InntektsmeldingMatcherDaoer,
    sessionFactory: TransactionalSessionFactory<SessionDaoerFelles>,
    private val inntektsmeldingClient: InntektsmeldingClient,
) {
    val db = DbDaoer(daoer, sessionFactory)
    val personDao = daoer.personDao

    suspend fun hentInntektsmeldingerForYrkesaktivitet(
        ref: YrkesaktivitetReferanse,
        fnr: String,
        saksbehandlerToken: SpilleromBearerToken,
    ): JsonNode {
        val yrkesaktivitet =
            db.nonTransactional {
                yrkesaktivitetDao.hentYrkesaktivitetDbRecord(ref.yrkesaktivitetUUID)!!.tilYrkesaktivitet()
            }
        if (yrkesaktivitet.kategorisering !is YrkesaktivitetKategorisering.Arbeidstaker) {
            throw BadRequestException("Kategorisering er ikke Arbeidstaker, da henter vi ikke inntektsmeldinger")
        }
        // TODO noe filterering på datoer og orgnummer trengs

        return inntektsmeldingClient.hentInntektsmeldinger(
            fnr = fnr,
            fom = null,
            tom = null,
            saksbehandlerToken = saksbehandlerToken,
        )
    }
}
