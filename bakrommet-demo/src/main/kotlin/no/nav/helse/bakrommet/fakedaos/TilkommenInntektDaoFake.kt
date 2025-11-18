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

    override fun oppdater(tilkommenInntektDbRecord: TilkommenInntektDbRecord): TilkommenInntektDbRecord {
        if (!storage.contains(tilkommenInntektDbRecord.id)) {
            throw IllegalArgumentException("TilkommenInntekt med id ${tilkommenInntektDbRecord.id} finnes ikke og kan derfor ikke oppdateres.")
        }
        storage[tilkommenInntektDbRecord.id] = tilkommenInntektDbRecord
        return tilkommenInntektDbRecord
    }

    override fun slett(
        behandlingId: UUID,
        id: UUID,
    ) {
        if (!storage.contains(id)) {
            throw IllegalArgumentException("TilkommenInntekt med id $id finnes ikke og kan derfor ikke slettes.")
        }
        storage.remove(id)
    }
}
