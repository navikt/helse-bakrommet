package no.nav.helse.bakrommet.sykepengesoknad

import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

class SoknaderService(
    private val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    private val personService: PersonService,
) {
    suspend fun hentSoknader(
        saksbehandlerToken: SpilleromBearerToken,
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        medSporsmal: Boolean = false,
    ): List<SykepengesoknadDTO> =
        sykepengesoknadBackendClient.hentSoknader(
            saksbehandlerToken = saksbehandlerToken,
            fnr = naturligIdent.naturligIdent,
            fom = fom,
            medSporsmal = medSporsmal,
        )

    suspend fun hentSoknad(
        saksbehandlerToken: SpilleromBearerToken,
        naturligIdent: NaturligIdent,
        soknadId: String,
    ): SykepengesoknadDTO {
        // Valider at personId er gyldig (hent fnr for å sjekke at personen eksisterer)
// TODO valider at søknad henger sammen med fnr?
        return sykepengesoknadBackendClient.hentSoknad(
            saksbehandlerToken = saksbehandlerToken,
            id = soknadId,
        )
    }
}
