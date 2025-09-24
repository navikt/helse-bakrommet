package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse

class UtbetalingsberegningService(
    private val beregningDao: UtbetalingsberegningDao,
) {
    fun hentUtbetalingsberegning(referanse: SaksbehandlingsperiodeReferanse): BeregningResponse? {
        return beregningDao.hentBeregning(referanse.periodeUUID)
    }
}
