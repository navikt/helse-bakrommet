@file:Suppress("ktlint:standard:filename", "ktlint:standard:class-naming")

package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.enkelBehandlingDbRecord
import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class IkkeOppfylt8_2IkkeVurdert8_47Test {
    @Test
    fun `8-2 ikke oppfylt, 8-47 ikke vurdert, Det er ikke OK`() {
        val data =
            ValideringData(
                behandlingDbRecord = enkelBehandlingDbRecord,
                yrkesaktiviteter = emptyList(),
                sykepengegrunnlag = null,
                beregningData = null,
                vurderteVilkår =
                    listOf(
                        VurdertVilkår(
                            id =
                                VilkårsvurderingId(
                                    behandlingId = BehandlingId(UUID.randomUUID()),
                                    vilkårskode = Vilkårskode("OPPTJENING"),
                                ),
                            vurdering =
                                VurdertVilkår.Vurdering(
                                    utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                                    underspørsmål = listOf(),
                                    notat = "",
                                ),
                        ),
                    ),
            )
        assertTrue(IkkeOppfylt8_2IkkeVurdert8_47.harInkonsistens(data))
    }

    @Test
    fun `8-2 ikke oppfylt, 8-47 ikke oppfylt, Det er helt OK`() {
        val data =
            ValideringData(
                behandlingDbRecord = enkelBehandlingDbRecord,
                yrkesaktiviteter = emptyList(),
                sykepengegrunnlag = null,
                beregningData = null,
                vurderteVilkår =
                    listOf(
                        VurdertVilkår(
                            id =
                                VilkårsvurderingId(
                                    behandlingId = BehandlingId(UUID.randomUUID()),
                                    vilkårskode = Vilkårskode("OPPTJENING"),
                                ),
                            vurdering =
                                VurdertVilkår.Vurdering(
                                    utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                                    underspørsmål = listOf(),
                                    notat = "",
                                ),
                        ),
                        VurdertVilkår(
                            id =
                                VilkårsvurderingId(
                                    behandlingId = BehandlingId(UUID.randomUUID()),
                                    vilkårskode = Vilkårskode("SYK_INAKTIV"),
                                ),
                            vurdering =
                                VurdertVilkår.Vurdering(
                                    utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                                    underspørsmål = listOf(),
                                    notat = "",
                                ),
                        ),
                    ),
            )
        assertFalse(IkkeOppfylt8_2IkkeVurdert8_47.harInkonsistens(data))
    }
}
