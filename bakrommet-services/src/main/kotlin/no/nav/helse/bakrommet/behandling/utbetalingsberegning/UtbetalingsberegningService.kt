package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer

interface UtbetalingsberegningDaoer {
    val beregningDao: UtbetalingsberegningDao
}

class UtbetalingsberegningService(
    private val db: DbDaoer<UtbetalingsberegningDaoer>,
) {
    suspend fun hentUtbetalingsberegning(referanse: SaksbehandlingsperiodeReferanse): BeregningResponse? = db.nonTransactional { beregningDao.hentBeregning(referanse.behandlingId) }
}
