package no.nav.helse.bakrommet.domain.saksbehandling.behandling

import no.nav.helse.bakrommet.domain.enBehandlingId
import no.nav.helse.bakrommet.domain.etVurdertVilkår
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VurdertVilkårTest {
    @Test
    fun `VurdertVilkår kan opprettes med initiell vurdering`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                vurdering = VurdertVilkår.Vurdering.OPPFYLT,
                notat = "Er oppfylt",
            )
        assertEquals(VurdertVilkår.Vurdering.OPPFYLT, vilkår.vurdering)
        assertEquals("Er oppfylt", vilkår.notat)
    }

    @Test
    fun `VurdertVilkår kan opprettes uten notat`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                vurdering = VurdertVilkår.Vurdering.IKKE_OPPFYLT,
                notat = null,
            )
        assertEquals(VurdertVilkår.Vurdering.IKKE_OPPFYLT, vilkår.vurdering)
        assertNull(vilkår.notat)
    }

    @Test
    fun `nyVurdering endrer vurdering og notat`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                vurdering = VurdertVilkår.Vurdering.OPPFYLT,
                notat = "Initiell",
            )
        vilkår.nyVurdering(VurdertVilkår.Vurdering.IKKE_OPPFYLT, "Endret vurdering")

        assertEquals(VurdertVilkår.Vurdering.IKKE_OPPFYLT, vilkår.vurdering)
        assertEquals("Endret vurdering", vilkår.notat)
    }

    @Test
    fun `nyVurdering kan sette notat til null`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                vurdering = VurdertVilkår.Vurdering.OPPFYLT,
                notat = "Initiell",
            )
        vilkår.nyVurdering(VurdertVilkår.Vurdering.IKKE_RELEVANT, null)

        assertEquals(VurdertVilkår.Vurdering.IKKE_RELEVANT, vilkår.vurdering)
        assertNull(vilkår.notat)
    }

    @Test
    fun `nyVurdering kan oppdatere vurdering flere ganger`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                vurdering = VurdertVilkår.Vurdering.OPPFYLT,
                notat = "Første",
            )
        vilkår.nyVurdering(VurdertVilkår.Vurdering.IKKE_OPPFYLT, "Andre")
        vilkår.nyVurdering(VurdertVilkår.Vurdering.SKAL_IKKE_VURDERES, "Tredje")

        assertEquals(VurdertVilkår.Vurdering.SKAL_IKKE_VURDERES, vilkår.vurdering)
        assertEquals("Tredje", vilkår.notat)
    }
}
