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
        val sykepengegrunnlag =
            periode.sykepengegrunnlagId?.let { sykepengegrunnlagDao.hentSykepengegrunnlag(it) }
    }
}
