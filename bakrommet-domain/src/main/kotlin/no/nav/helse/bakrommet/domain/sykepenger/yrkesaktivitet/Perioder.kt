package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.sykepenger.Periode

data class Perioder(
    val type: Periodetype,
    val perioder: List<Periode>,
)

enum class Periodetype {
    ARBEIDSGIVERPERIODE,
    VENTETID,
    VENTETID_INAKTIV,
}
