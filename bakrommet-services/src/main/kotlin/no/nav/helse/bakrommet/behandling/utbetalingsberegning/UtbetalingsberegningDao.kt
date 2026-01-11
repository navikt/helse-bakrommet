package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.auth.Bruker
import java.util.UUID

interface UtbetalingsberegningDao {
    fun settBeregning(
        behandlingId: UUID,
        beregning: BeregningResponse,
        saksbehandler: Bruker,
    ): BeregningResponse

    fun hentBeregning(behandlingId: UUID): BeregningResponse?

    fun slettBeregning(
        behandlingId: UUID,
        failSilently: Boolean = false,
    )
}
