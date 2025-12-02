package no.nav.helse.bakrommet.api.bruker

import no.nav.helse.bakrommet.api.dto.bruker.BrukerDto
import no.nav.helse.bakrommet.api.dto.bruker.RolleDto
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.Rolle

fun Bruker.tilBrukerDto(): BrukerDto =
    BrukerDto(
        navn = navn,
        navIdent = navIdent,
        preferredUsername = preferredUsername,
        roller = roller.map { it.tilRolleDto() }.toSet(),
    )

private fun Rolle.tilRolleDto(): RolleDto =
    when (this) {
        Rolle.LES -> RolleDto.LES
        Rolle.SAKSBEHANDLER -> RolleDto.SAKSBEHANDLER
        Rolle.BESLUTTER -> RolleDto.BESLUTTER
    }
