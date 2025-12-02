package no.nav.helse.bakrommet.api.person

import no.nav.helse.bakrommet.api.dto.person.PersonsøkResponseDto
import no.nav.helse.bakrommet.person.SpilleromPersonId

fun SpilleromPersonId.tilPersonsøkResponseDto(): PersonsøkResponseDto =
    PersonsøkResponseDto(
        personId = personId,
    )
