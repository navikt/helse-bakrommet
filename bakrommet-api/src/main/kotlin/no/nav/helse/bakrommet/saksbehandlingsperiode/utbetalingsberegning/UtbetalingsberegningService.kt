package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse

class UtbetalingsberegningService(
    private val beregningDao: UtbetalingsberegningDao,
) {
    suspend fun hentUtbetalingsberegning(referanse: SaksbehandlingsperiodeReferanse): BeregningResponse? = beregningDao.hentBeregning(referanse.periodeUUID)
}
