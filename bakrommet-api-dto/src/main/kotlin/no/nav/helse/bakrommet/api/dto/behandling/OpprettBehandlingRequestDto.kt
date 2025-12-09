package no.nav.helse.bakrommet.api.dto.behandling

import java.time.LocalDate
import java.util.UUID

data class OpprettBehandlingRequestDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val søknader: List<UUID>? = null,
)
