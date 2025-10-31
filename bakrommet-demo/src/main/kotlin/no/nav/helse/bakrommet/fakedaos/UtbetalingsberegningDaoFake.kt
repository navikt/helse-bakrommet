package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.BeregningResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UtbetalingsberegningDaoFake : UtbetalingsberegningDao {
    private val storage = ConcurrentHashMap<UUID, BeregningResponse>()

    override fun settBeregning(
        saksbehandlingsperiodeId: UUID,
        beregning: BeregningResponse,
        saksbehandler: Bruker,
    ): BeregningResponse {
        val oppdatert =
            beregning.copy(
                saksbehandlingsperiodeId = saksbehandlingsperiodeId,
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = OffsetDateTime.now().toString(),
            )
        storage[saksbehandlingsperiodeId] = oppdatert
        return oppdatert
    }

    override fun hentBeregning(saksbehandlingsperiodeId: UUID): BeregningResponse? = storage[saksbehandlingsperiodeId]

    override fun slettBeregning(saksbehandlingsperiodeId: UUID) {
        storage.remove(saksbehandlingsperiodeId)
    }
}
