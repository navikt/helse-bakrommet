package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DokumentDaoFake : DokumentDao {
    private val dokumenter = ConcurrentHashMap<UUID, Dokument>()

    override fun finnDokumentMedEksternId(
        behandlingId: UUID,
        dokumentType: String,
        eksternId: String,
    ): Dokument? =
        dokumenter.values.firstOrNull {
            it.opprettetForBehandling == behandlingId &&
                it.dokumentType == dokumentType &&
                it.eksternId == eksternId
        }

    override fun finnDokumentForForespurteData(
        behandlingId: UUID,
        dokumentType: String,
        forespurteData: String,
    ): Dokument? =
        dokumenter.values.firstOrNull {
            it.opprettetForBehandling == behandlingId &&
                it.dokumentType == dokumentType &&
                it.forespurteData == forespurteData
        }

    override fun opprettDokument(dokument: Dokument): Dokument {
        dokumenter[dokument.id] = dokument
        return dokument
    }

    override fun hentDokument(id: UUID): Dokument? = dokumenter[id]

    override fun hentDokumenterFor(behandlingId: UUID): List<Dokument> = dokumenter.values.filter { it.opprettetForBehandling == behandlingId }
}
