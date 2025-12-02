package no.nav.helse.bakrommet.api.dto.behandling

import java.util.UUID

data class CreatePeriodeRequestDto(
    val fom: String,
    val tom: String,
    val søknader: List<UUID>? = null,
)
