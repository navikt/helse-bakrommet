package no.nav.helse.bakrommet.api.dokumenter

import no.nav.helse.bakrommet.api.dto.dokumenter.DokumentDto
import no.nav.helse.bakrommet.api.dto.dokumenter.KildesporDto
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.util.asJsonNode

fun Dokument.tilDokumentDto(): DokumentDto =
    DokumentDto(
        id = id,
        dokumentType = dokumentType,
        eksternId = eksternId,
        innhold = innhold.asJsonNode(),
        opprettet = opprettet,
        request = sporing.tilKildesporDto(),
    )

private fun no.nav.helse.bakrommet.util.Kildespor.tilKildesporDto(): KildesporDto = KildesporDto(kilde = kilde)
