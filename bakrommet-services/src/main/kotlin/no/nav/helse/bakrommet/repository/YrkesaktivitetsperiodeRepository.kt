package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetsperiodeId

interface YrkesaktivitetsperiodeRepository {
    fun finn(behandlingId: BehandlingId): List<Yrkesaktivitetsperiode>

    fun finn(yrkesaktivitetsperiodeId: YrkesaktivitetsperiodeId): Yrkesaktivitetsperiode?

    fun lagre(yrkesaktivitetsperiode: Yrkesaktivitetsperiode)

    fun slett(yrkesaktivitetsperiodeId: YrkesaktivitetsperiodeId)
}
