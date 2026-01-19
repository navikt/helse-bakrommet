package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.KildeDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.RefusjonsperiodeDto
import no.nav.helse.bakrommet.behandling.dagoversikt.Kilde
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode

fun Kilde.tilKildeDto(): KildeDto =
    when (this) {
        Kilde.Søknad -> KildeDto.Søknad
        Kilde.Saksbehandler -> KildeDto.Saksbehandler
    }

fun Refusjonsperiode.tilRefusjonsperiodeDto(): RefusjonsperiodeDto =
    RefusjonsperiodeDto(
        fom = fom,
        tom = tom,
        beløp = beløp.beløp,
    )
