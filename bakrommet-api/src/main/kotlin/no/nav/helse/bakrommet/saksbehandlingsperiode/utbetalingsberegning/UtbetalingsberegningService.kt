package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse

interface UtbetalingsberegningDaoer {
    val beregningDao: UtbetalingsberegningDao
}

class UtbetalingsberegningService(
    private val db: DbDaoer<UtbetalingsberegningDaoer>,
) {
    suspend fun hentUtbetalingsberegning(referanse: SaksbehandlingsperiodeReferanse): BeregningResponse? = db.nonTransactional { beregningDao.hentBeregning(referanse.periodeUUID) }
}
