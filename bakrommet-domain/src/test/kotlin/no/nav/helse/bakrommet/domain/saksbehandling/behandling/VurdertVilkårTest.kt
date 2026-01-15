package no.nav.helse.bakrommet.domain.saksbehandling.behandling

import no.nav.helse.bakrommet.domain.enBehandlingId
import no.nav.helse.bakrommet.domain.etVurdertVilkår
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class VurdertVilkårTest {
    @Test
    fun `VurdertVilkår kan opprettes med initiell vurdering`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                utfall = VurdertVilkår.Utfall.OPPFYLT,
                notat = "Er oppfylt",
            )
        assertEquals(VurdertVilkår.Utfall.OPPFYLT, vilkår.vurdering.utfall)
        assertEquals("Er oppfylt", vilkår.vurdering.notat)
    }

    @Test
    fun `VurdertVilkår kan opprettes uten notat`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                notat = null,
            )
        assertEquals(VurdertVilkår.Utfall.IKKE_OPPFYLT, vilkår.vurdering.utfall)
        assertNull(vilkår.vurdering.notat)
    }

    @Test
    fun `nyVurdering endrer vurdering og notat`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                utfall = VurdertVilkår.Utfall.OPPFYLT,
                notat = "Initiell",
            )
        vilkår.nyVurdering(
            VurdertVilkår.Vurdering(
                underspørsmål = emptyList(),
                notat = "Endret vurdering",
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
            ),
        )

        assertEquals(VurdertVilkår.Utfall.IKKE_OPPFYLT, vilkår.vurdering.utfall)
        assertEquals("Endret vurdering", vilkår.vurdering.notat)
    }

    @Test
    fun `nyVurdering kan sette notat til null`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                utfall = VurdertVilkår.Utfall.OPPFYLT,
                notat = "Initiell",
            )
        vilkår.nyVurdering(
            VurdertVilkår.Vurdering(
                underspørsmål = emptyList(),
                notat = null,
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
            ),
        )

        assertNull(vilkår.vurdering.notat)
    }

    @Test
    fun `nyVurdering kan oppdatere vurdering flere ganger`() {
        val vilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                utfall = VurdertVilkår.Utfall.OPPFYLT,
                notat = "Første",
            )
        vilkår.nyVurdering(
            VurdertVilkår.Vurdering(
                underspørsmål = emptyList(),
                notat = "andre",
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
            ),
        )
        vilkår.nyVurdering(
            VurdertVilkår.Vurdering(
                underspørsmål =
                    listOf(
                        VilkårsvurderingUnderspørsmål(
                            spørsmål = "Spørsmål 1",
                            svar = "Svar 1",
                        ),
                        VilkårsvurderingUnderspørsmål(
                            spørsmål = "Spørsmål 2",
                            svar = "Svar 2",
                        ),
                    ),
                notat = "Tredje",
                utfall = VurdertVilkår.Utfall.SKAL_IKKE_VURDERES,
            ),
        )

        assertEquals(VurdertVilkår.Utfall.SKAL_IKKE_VURDERES, vilkår.vurdering.utfall)
        assertEquals("Tredje", vilkår.vurdering.notat)
        assertEquals(2, vilkår.vurdering.underspørsmål.size)
    }

    @Test
    fun `kopierTil bygger nytt vilkår med ny behandlingId og samme vurdering`() {
        val originalVilkår =
            etVurdertVilkår(
                behandlingId = enBehandlingId(),
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                notat = "Original",
            )
        val nyBehandlingId = enBehandlingId()

        val kopi = originalVilkår.kopierTil(nyBehandlingId)

        assertEquals(nyBehandlingId, kopi.id.behandlingId)
        assertEquals(originalVilkår.id.vilkårskode, kopi.id.vilkårskode)
        assertNotEquals(originalVilkår.id.behandlingId, kopi.id.behandlingId)
        assertEquals(originalVilkår.vurdering, kopi.vurdering)
    }
}
