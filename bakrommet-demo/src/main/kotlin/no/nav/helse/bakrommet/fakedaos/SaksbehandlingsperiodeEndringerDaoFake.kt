package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndring
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringerDao
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SaksbehandlingsperiodeEndringerDaoFake : SaksbehandlingsperiodeEndringerDao {
    private val endringer = ConcurrentHashMap<UUID, MutableList<SaksbehandlingsperiodeEndring>>()

    override fun leggTilEndring(hist: SaksbehandlingsperiodeEndring) {
        endringer.computeIfAbsent(hist.saksbehandlingsperiodeId) { mutableListOf() }.add(hist)
    }

    override fun hentEndringerFor(saksbehandlingsperiodeId: UUID): List<SaksbehandlingsperiodeEndring> = endringer[saksbehandlingsperiodeId]?.toList() ?: emptyList()
}
