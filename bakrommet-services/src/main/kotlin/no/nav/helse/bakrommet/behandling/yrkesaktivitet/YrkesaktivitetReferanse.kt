package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import java.util.*

data class YrkesaktivitetReferanse(
    val behandlingReferanse: BehandlingReferanse,
    val yrkesaktivitetUUID: UUID,
)
