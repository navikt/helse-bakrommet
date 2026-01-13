package no.nav.helse.bakrommet.sykepengesoknad

import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.infrastruktur.provider.SykepengesøknadProvider
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

class SoknaderService(
    private val sykepengesøknadProvider: SykepengesøknadProvider,
) {
    suspend fun hentSoknader(
        saksbehandlerToken: AccessToken,
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        medSporsmal: Boolean = false,
    ): List<SykepengesoknadDTO> =
        sykepengesøknadProvider.hentSoknader(
            saksbehandlerToken = saksbehandlerToken,
            fnr = naturligIdent.value,
            fom = fom,
            medSporsmal = medSporsmal,
        )

    suspend fun hentSoknad(
        saksbehandlerToken: AccessToken,
        soknadId: String,
    ): SykepengesoknadDTO {
        // Valider at personId er gyldig (hent fnr for å sjekke at personen eksisterer)
// TODO valider at søknad henger sammen med fnr?
        return sykepengesøknadProvider.hentSoknad(
            saksbehandlerToken = saksbehandlerToken,
            id = soknadId,
        )
    }
}
