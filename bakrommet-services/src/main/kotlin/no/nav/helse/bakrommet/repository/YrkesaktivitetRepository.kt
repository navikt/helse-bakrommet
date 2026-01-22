package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode

interface YrkesaktivitetRepository {
    fun finn(behandlingId: BehandlingId): List<Yrkesaktivitetsperiode>

    fun finn(yrkesaktivitetId: YrkesaktivitetId): Yrkesaktivitetsperiode?

    fun lagre(yrkesaktivitetsperiode: Yrkesaktivitetsperiode)

    fun slett(yrkesaktivitetId: YrkesaktivitetId)
}
