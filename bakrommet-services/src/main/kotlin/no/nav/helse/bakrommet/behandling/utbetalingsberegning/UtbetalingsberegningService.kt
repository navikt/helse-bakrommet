package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer

interface UtbetalingsberegningDaoer {
    val beregningDao: UtbetalingsberegningDao
}

class UtbetalingsberegningService(
    private val db: DbDaoer<UtbetalingsberegningDaoer>,
) {
    suspend fun hentUtbetalingsberegning(referanse: BehandlingReferanse): BeregningResponse? = db.nonTransactional { beregningDao.hentBeregning(referanse.behandlingId) }
}
