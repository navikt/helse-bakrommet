package no.nav.helse.bakrommet.api.organisasjon

import no.nav.helse.bakrommet.api.dto.organisasjon.OrganisasjonDto
import no.nav.helse.bakrommet.ereg.Organisasjon

fun Organisasjon.tilOrganisasjonDto(): OrganisasjonDto =
    OrganisasjonDto(
        navn = navn,
        orgnummer = orgnummer,
    )
