package no.nav.helse.bakrommet.api.person

import no.nav.helse.bakrommet.api.dto.person.PersoninfoResponseDto
import no.nav.helse.bakrommet.person.PersonInfo

fun PersonInfo.tilPersoninfoResponseDto(): PersoninfoResponseDto =
    PersoninfoResponseDto(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        navn = navn,
        alder = alder,
    )
