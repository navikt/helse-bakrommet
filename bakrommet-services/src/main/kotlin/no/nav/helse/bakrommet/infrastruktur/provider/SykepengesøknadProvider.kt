package no.nav.helse.bakrommet.infrastruktur.provider

import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

interface SykepengesøknadProvider {
    suspend fun hentSoknad(
        saksbehandlerToken: SpilleromBearerToken,
        id: String,
    ): SykepengesoknadDTO

    suspend fun hentSoknader(
        saksbehandlerToken: SpilleromBearerToken,
        fnr: String,
        fom: LocalDate,
        medSporsmal: Boolean = false,
    ): List<SykepengesoknadDTO>

    suspend fun hentSoknadMedSporing(
        saksbehandlerToken: SpilleromBearerToken,
        id: String,
    ): Pair<SykepengesoknadDTO, Kildespor>
}
