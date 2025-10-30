package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DokumentDaoFake : DokumentDao {
    private val dokumenter = ConcurrentHashMap<UUID, Dokument>()

    override fun finnDokumentMedEksternId(
        saksbehandlingsperiodeId: UUID,
        dokumentType: String,
        eksternId: String,
    ): Dokument? =
        dokumenter.values.firstOrNull {
            it.opprettetForBehandling == saksbehandlingsperiodeId &&
                it.dokumentType == dokumentType &&
                it.eksternId == eksternId
        }

    override fun finnDokumentForForespurteData(
        saksbehandlingsperiodeId: UUID,
        dokumentType: String,
        forespurteData: String,
    ): Dokument? =
        dokumenter.values.firstOrNull {
            it.opprettetForBehandling == saksbehandlingsperiodeId &&
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
