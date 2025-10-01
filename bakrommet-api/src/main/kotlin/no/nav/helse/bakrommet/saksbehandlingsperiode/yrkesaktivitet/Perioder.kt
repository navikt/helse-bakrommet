package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.dto.PeriodeDto

data class Perioder(
    val type: Periodetype,
    val perioder: List<PeriodeDto>,
)
