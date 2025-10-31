package no.nav.helse.bakrommet.fakedaos

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.hentSessionid
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.BeregningResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class UtbetalingsberegningDaoFake : UtbetalingsberegningDao {
    // Map av sessionId -> saksbehandlingsperiodeId -> BeregningResponse
    private val sessionData = ConcurrentHashMap<String, ConcurrentHashMap<UUID, BeregningResponse>>()

    private fun getSessionMap(): ConcurrentHashMap<UUID, BeregningResponse> =
        runBlocking {
            val sessionId = hentSessionid()

            sessionData.getOrPut(sessionId) { ConcurrentHashMap() }
        }

    private val storage: ConcurrentHashMap<UUID, BeregningResponse>
        get() = getSessionMap()

    override suspend fun settBeregning(
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

    override suspend fun hentBeregning(saksbehandlingsperiodeId: UUID): BeregningResponse? = storage[saksbehandlingsperiodeId]

    override suspend fun slettBeregning(saksbehandlingsperiodeId: UUID) {
        storage.remove(saksbehandlingsperiodeId)
    }
}
