package no.nav.helse.bakrommet.api.person

import no.nav.helse.bakrommet.api.dto.person.PersonsøkResponseDto
import java.util.UUID

fun UUID.tilPersonsøkResponseDto(): PersonsøkResponseDto =
    PersonsøkResponseDto(
        personId = this.toString(),
    )
