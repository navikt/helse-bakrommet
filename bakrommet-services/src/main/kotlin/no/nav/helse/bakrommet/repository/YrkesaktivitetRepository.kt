package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetId

interface YrkesaktivitetRepository {
    fun finn(behandlingId: BehandlingId): List<Yrkesaktivitet>

    fun finn(yrkesaktivitetId: YrkesaktivitetId): Yrkesaktivitet?

    fun lagre(yrkesaktivitet: Yrkesaktivitet)

    fun slett(yrkesaktivitetId: YrkesaktivitetId)
}
