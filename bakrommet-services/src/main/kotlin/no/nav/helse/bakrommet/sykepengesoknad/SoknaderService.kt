package no.nav.helse.bakrommet.sykepengesoknad

import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

class SoknaderService(
    private val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    private val personService: PersonService,
) {
    suspend fun hentSoknader(
        saksbehandlerToken: SpilleromBearerToken,
        personId: SpilleromPersonId,
        fom: LocalDate,
        medSporsmal: Boolean = false,
    ): List<SykepengesoknadDTO> {
        val fnr =
            personService.finnNaturligIdent(personId.personId)
                ?: throw IllegalArgumentException("Kunne ikke finne fødselsnummer for person $personId")
        return sykepengesoknadBackendClient.hentSoknader(
            saksbehandlerToken = saksbehandlerToken,
            fnr = fnr,
            fom = fom,
            medSporsmal = medSporsmal,
        )
    }

    suspend fun hentSoknad(
        saksbehandlerToken: SpilleromBearerToken,
        personId: SpilleromPersonId,
        soknadId: String,
    ): SykepengesoknadDTO {
        // Valider at personId er gyldig (hent fnr for å sjekke at personen eksisterer)
        personService.finnNaturligIdent(personId.personId)
            ?: throw IllegalArgumentException("Kunne ikke finne fødselsnummer for person $personId")
        return sykepengesoknadBackendClient.hentSoknad(
            saksbehandlerToken = saksbehandlerToken,
            id = soknadId,
        )
    }
}
