package no.nav.helse.bakrommet.fakedaos

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.hentSessionid
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DokumentDaoFake : DokumentDao {
    // Map av sessionId -> dokumentId -> Dokument
    private val sessionData = ConcurrentHashMap<String, ConcurrentHashMap<UUID, Dokument>>()

    private fun getSessionMap(): ConcurrentHashMap<UUID, Dokument> =
        runBlocking {
            val sessionId = hentSessionid()

            sessionData.getOrPut(sessionId) { ConcurrentHashMap() }
        }

    private val dokumenter: ConcurrentHashMap<UUID, Dokument>
        get() = getSessionMap()

    override suspend fun finnDokumentMedEksternId(
        saksbehandlingsperiodeId: UUID,
        dokumentType: String,
        eksternId: String,
    ): Dokument? =
        dokumenter.values.firstOrNull {
            it.opprettetForBehandling == saksbehandlingsperiodeId &&
                it.dokumentType == dokumentType &&
                it.eksternId == eksternId
        }

    override suspend fun finnDokumentForForespurteData(
        saksbehandlingsperiodeId: UUID,
        dokumentType: String,
        forespurteData: String,
    ): Dokument? =
        dokumenter.values.firstOrNull {
            it.opprettetForBehandling == saksbehandlingsperiodeId &&
                it.dokumentType == dokumentType &&
                it.forespurteData == forespurteData
        }

    override suspend fun opprettDokument(dokument: Dokument): Dokument {
        dokumenter[dokument.id] = dokument
        return dokument
    }

    override suspend fun hentDokument(id: UUID): Dokument? = dokumenter[id]

    override suspend fun hentDokumenterFor(behandlingId: UUID): List<Dokument> = dokumenter.values.filter { it.opprettetForBehandling == behandlingId }
}
