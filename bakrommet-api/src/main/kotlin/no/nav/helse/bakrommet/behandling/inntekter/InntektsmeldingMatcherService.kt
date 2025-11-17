package no.nav.helse.bakrommet.behandling.inntekter

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.plugins.BadRequestException
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.tilYrkesaktivitet
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.person.PersonDao

interface InntektsmeldingMatcherDaoer {
    val behandlingDao: BehandlingDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val beregningDao: UtbetalingsberegningDao
    val personDao: PersonDao
}

class InntektsmeldingMatcherService(
    val db: DbDaoer<InntektsmeldingMatcherDaoer>,
    private val inntektsmeldingClient: InntektsmeldingClient,
) {
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
