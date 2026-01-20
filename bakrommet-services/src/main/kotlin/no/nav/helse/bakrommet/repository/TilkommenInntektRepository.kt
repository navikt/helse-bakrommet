package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektId

interface TilkommenInntektRepository {
    fun lagre(tilkommenInntekt: TilkommenInntekt)

    fun finn(tilkommenInntektId: TilkommenInntektId): TilkommenInntekt?

    fun slett(tilkommenInntektId: TilkommenInntektId)

    fun finnFor(behandlingId: BehandlingId): List<TilkommenInntekt>
}
