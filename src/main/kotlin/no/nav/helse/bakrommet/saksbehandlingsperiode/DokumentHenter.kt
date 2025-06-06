package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import java.util.*

class DokumentHenter(
    private val personDao: PersonDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val dokumentDao: DokumentDao,
    private val soknadClient: SykepengesoknadBackendClient,
) {
    fun hentOgLagreSøknaderOgInntekter(
        saksbehandlingsperiodeId: UUID,
        søknadsIder: List<UUID>,
    ) {
        if (søknadsIder.isEmpty()) return
        val periode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(saksbehandlingsperiodeId)
        requireNotNull(periode) { "Fant ikke saksbehandlingsperiode med id=$saksbehandlingsperiodeId" }

        val fnr = personDao.finnNaturligIdent(periode.spilleromPersonId)
        søknadsIder.forEach { søknadId ->
            // soknadClient.hentSoknad()
        }
    }
}
