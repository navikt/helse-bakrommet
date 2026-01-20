package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDao
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDbRecord
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TilkommenInntektDaoFake : TilkommenInntektDao {
    private val storage = ConcurrentHashMap<UUID, TilkommenInntektDbRecord>()

    override fun opprett(tilkommenInntektDbRecord: TilkommenInntektDbRecord): TilkommenInntektDbRecord {
        storage[tilkommenInntektDbRecord.id] = tilkommenInntektDbRecord
        return tilkommenInntektDbRecord
    }

    override fun hentForBehandling(behandlingId: UUID): List<TilkommenInntektDbRecord> = storage.values.filter { it.behandlingId == behandlingId }

    override fun hent(id: UUID): TilkommenInntektDbRecord? = storage[id]

    override fun finnTilkommenInntektForBehandlinger(map: List<UUID>): List<TilkommenInntektDbRecord> = storage.values.filter { map.contains(it.behandlingId) }
}
