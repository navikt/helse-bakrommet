package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet

interface YrkesaktivitetRepository {
    fun finn(behandlingId: BehandlingId): List<Yrkesaktivitet>

    fun lagre(yrkesaktivitet: Yrkesaktivitet)
}
