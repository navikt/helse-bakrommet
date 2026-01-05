package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.BeregningResponse
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDao
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UtbetalingsberegningDaoFake : UtbetalingsberegningDao {
    private val storage = ConcurrentHashMap<UUID, BeregningResponse>()

    override fun settBeregning(
        behandlingId: UUID,
        beregning: BeregningResponse,
        saksbehandler: Bruker,
    ): BeregningResponse {
        val oppdatert =
            beregning.copy(
                behandlingId = behandlingId,
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = OffsetDateTime.now().toString(),
            )
        storage[behandlingId] = oppdatert
        return oppdatert
    }

    override fun hentBeregning(behandlingId: UUID): BeregningResponse? = storage[behandlingId]

    override fun slettBeregning(
        behandlingId: UUID,
        failSilently: Boolean,
    ) {
        storage.remove(behandlingId)
    }
}
