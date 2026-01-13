package no.nav.helse.bakrommet.infrastruktur.provider

import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

interface SykepengesøknadProvider {
    suspend fun hentSoknad(
        saksbehandlerToken: AccessToken,
        id: String,
    ): SykepengesoknadDTO

    suspend fun hentSoknader(
        saksbehandlerToken: AccessToken,
        fnr: String,
        fom: LocalDate,
        medSporsmal: Boolean = false,
    ): List<SykepengesoknadDTO>

    suspend fun hentSoknadMedSporing(
        saksbehandlerToken: AccessToken,
        id: String,
    ): Pair<SykepengesoknadDTO, Kildespor>
}
