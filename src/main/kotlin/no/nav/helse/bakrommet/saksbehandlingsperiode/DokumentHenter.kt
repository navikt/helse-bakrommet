package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.util.*
import java.util.*

class DokumentHenter(
    private val personDao: PersonDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val dokumentDao: DokumentDao,
    private val soknadClient: SykepengesoknadBackendClient,
) {
    companion object {
        object DokumentType {
            val søknad = "søknad"
            val aInntekt828 = "ainntekt828"
        }
    }

    suspend fun hentOgLagreSøknaderOgInntekter(
        saksbehandlingsperiodeId: UUID,
        søknadsIder: List<UUID>,
        spilleromBearerToken: SpilleromBearerToken,
    ) {
        if (søknadsIder.isEmpty()) return
        val periode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(saksbehandlingsperiodeId)
        requireNotNull(periode) { "Fant ikke saksbehandlingsperiode med id=$saksbehandlingsperiodeId" }

        val fnr = personDao.finnNaturligIdent(periode.spilleromPersonId)
        søknadsIder.forEach { søknadId ->
            logg.info("Henter søknad med id={}", søknadId)

            val request = Kildespor.fraHer(Throwable(), søknadId)
            val søknadDto =
                soknadClient.hentSoknad(
                    saksbehandlerToken = spilleromBearerToken,
                    id = søknadId.toString(),
                )

            dokumentDao.opprettDokument(
                Dokument(
                    dokumentType = DokumentType.søknad,
                    eksternId = søknadId.toString(),
                    innhold = søknadDto.serialisertTilString(),
                    request = request,
                    opprettetForBehandling = saksbehandlingsperiodeId,
                ),
            )
        }
    }
}
