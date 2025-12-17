package no.nav.helse.bakrommet.behandling.inntekter

import io.ktor.server.plugins.BadRequestException
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somInntektsmeldingObjektListe
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.maybeOrgnummer
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.tilYrkesaktivitet
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.person.PersonPseudoIdDao
import no.nav.inntektsmeldingkontrakt.Inntektsmelding

interface InntektsmeldingMatcherDaoer {
    val behandlingDao: BehandlingDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val beregningDao: UtbetalingsberegningDao
    val personPseudoIdDao: PersonPseudoIdDao
}

class InntektsmeldingMatcherService(
    val db: DbDaoer<InntektsmeldingMatcherDaoer>,
    private val inntektsmeldingClient: InntektsmeldingClient,
) {
    suspend fun hentInntektsmeldingerForYrkesaktivitet(
        ref: YrkesaktivitetReferanse,
        fnr: String,
        bruker: BrukerOgToken,
    ): List<Inntektsmelding> {
        val (behandling, yrkesaktivitet) =
            db.nonTransactional {
                val yrkesaktivitet =
                    yrkesaktivitetDao.hentYrkesaktivitetDbRecord(ref.yrkesaktivitetUUID)!!.tilYrkesaktivitet()
                val behandling =
                    behandlingDao.hentPeriode(ref.behandlingReferanse, krav = bruker.bruker.erSaksbehandlerPåSaken())
                behandling to yrkesaktivitet
            }

        if (yrkesaktivitet.kategorisering !is YrkesaktivitetKategorisering.Arbeidstaker) {
            throw BadRequestException("Kategorisering er ikke Arbeidstaker, da henter vi ikke inntektsmeldinger")
        }

        val inntektsmeldinger =
            inntektsmeldingClient.hentInntektsmeldinger(
                fnr = fnr,
                fom = null,
                tom = null,
                saksbehandlerToken = bruker.token,
            )
        return inntektsmeldinger
            .somInntektsmeldingObjektListe()
            .filter { it.virksomhetsnummer == yrkesaktivitet.kategorisering.maybeOrgnummer() }
            .filter { it.foersteFravaersdag != null } // må ha fraværsdag for å matche
            .filter { it.foersteFravaersdag!!.isAfter(behandling.skjæringstidspunkt.minusWeeks(4)) } // må ha fraværsdag for å matche
            .filter { it.foersteFravaersdag!!.isBefore(behandling.skjæringstidspunkt.plusWeeks(4)) }
        // TODO hvis vurder også matching mot yrkesaktivitetens start
    }
}
