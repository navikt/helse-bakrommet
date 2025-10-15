package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao

class SykepengegrunnlagBeregningHjelper(
    private val beregningDao: UtbetalingsberegningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetDao: YrkesaktivitetDao,
) {
    fun beregnSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ) {
        // Hent nødvendige data for beregningen
        val periode =
            saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(referanse.periodeUUID)
                ?: throw RuntimeException("Fant ikke saksbehandlingsperiode for id ${referanse.periodeUUID}")

        // TODO valider at sb på saken? Eller anta at det skjer senere

        // Hent sykepengegrunnlag
        val eksisterendeSykepengegrunnlag =
            periode.sykepengegrunnlagId?.let { sykepengegrunnlagDao.hentSykepengegrunnlag(it) }

        val yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktivitetFor(periode)
        val harFullInnntektsdata = yrkesaktiviteter.all { it.inntektData != null }
        if (!harFullInnntektsdata && (eksisterendeSykepengegrunnlag != null) && (eksisterendeSykepengegrunnlag.sykepengegrunnlag != null)) {
            sykepengegrunnlagDao.oppdaterSykepengrgrunnlag(eksisterendeSykepengegrunnlag.id, null)
        }

        // Vi har full data og kan beregne.
        // Først så beregnerer vi rariteten på næringsdrivende ved å trekke fra de andre årsinntektene fra pensjonsgivende inntekt
    }
}
