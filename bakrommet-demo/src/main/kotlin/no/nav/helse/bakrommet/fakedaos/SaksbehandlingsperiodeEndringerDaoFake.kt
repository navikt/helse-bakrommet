package no.nav.helse.bakrommet.fakedaos

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.hentSessionid
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeEndring
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeEndringerDao
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SaksbehandlingsperiodeEndringerDaoFake : SaksbehandlingsperiodeEndringerDao {
    // Map av sessionId -> periodeId -> liste av endringer
    private val sessionData = ConcurrentHashMap<String, ConcurrentHashMap<UUID, MutableList<SaksbehandlingsperiodeEndring>>>()

    private fun getSessionMap(): ConcurrentHashMap<UUID, MutableList<SaksbehandlingsperiodeEndring>> =
        runBlocking {
            val sessionId = hentSessionid()

            sessionData.getOrPut(sessionId) { ConcurrentHashMap() }
        }

    private val endringer: ConcurrentHashMap<UUID, MutableList<SaksbehandlingsperiodeEndring>>
        get() = getSessionMap()

    override suspend fun leggTilEndring(hist: SaksbehandlingsperiodeEndring) {
        endringer.computeIfAbsent(hist.saksbehandlingsperiodeId) { mutableListOf() }.add(hist)
    }

    override suspend fun hentEndringerFor(saksbehandlingsperiodeId: UUID): List<SaksbehandlingsperiodeEndring> = endringer[saksbehandlingsperiodeId]?.toList() ?: emptyList()
}
