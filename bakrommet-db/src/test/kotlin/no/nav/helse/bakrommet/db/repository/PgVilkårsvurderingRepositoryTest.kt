package no.nav.helse.bakrommet.db.repository

import kotliquery.sessionOf
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.domain.enBehandling
import no.nav.helse.bakrommet.domain.etVurdertVilkår
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PgVilkårsvurderingRepositoryTest {
    private val dataSource = TestDataSource.dbModule.dataSource
    private val session = sessionOf(dataSource)
    private val repository = PgVilkårsvurderingRepository(session)
    private val behandlingRepository = PgBehandlingRepository(session)

    @Test
    fun `lagre og finn`() {
        // given
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår = etVurdertVilkår(behandlingId = behandling.id)

        // when
        repository.lagre(vurdertVilkår)

        // then
        val funnet = repository.finn(vurdertVilkår.id)
        assertNotNull(funnet)

        assertEquals(vurdertVilkår.id, funnet.id)
        assertEquals(vurdertVilkår.vurdering.utfall, funnet.vurdering.utfall)
        assertEquals(vurdertVilkår.vurdering.underspørsmål, funnet.vurdering.underspørsmål)
        assertEquals(vurdertVilkår.vurdering.notat, funnet.vurdering.notat)
    }

    @Test
    fun `finn returnerer null når vilkårsvurdering ikke finnes`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val vilkårsvurderingId =
            VilkårsvurderingId(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("OPPTJENING"),
            )

        val funnet = repository.finn(vilkårsvurderingId)
        assertNull(funnet)
    }

    @Test
    fun `oppdater eksisterende vilkårsvurdering`() {
        // given
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår = etVurdertVilkår(behandlingId = behandling.id)
        repository.lagre(vurdertVilkår)

        // when
        vurdertVilkår.nyVurdering(
            VurdertVilkår.Vurdering(
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                underspørsmål = vurdertVilkår.vurdering.underspørsmål,
                notat = "Oppdatert notat",
            ),
        )
        repository.lagre(vurdertVilkår)

        // then
        val funnet = repository.finn(vurdertVilkår.id)
        assertNotNull(funnet)

        assertEquals(vurdertVilkår.id, funnet.id)
        assertEquals(VurdertVilkår.Utfall.IKKE_OPPFYLT, funnet.vurdering.utfall)
        assertEquals("Oppdatert notat", funnet.vurdering.notat)
        assertEquals(vurdertVilkår.vurdering.underspørsmål, funnet.vurdering.underspørsmål)
    }

    @Test
    fun `lagre vilkårsvurdering med underspørsmål`() {
        // given
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val underspørsmål =
            listOf(
                VilkårsvurderingUnderspørsmål("Første spørsmål?", "Første svar"),
                VilkårsvurderingUnderspørsmål("Andre spørsmål?", "Andre svar"),
                VilkårsvurderingUnderspørsmål("Tredje spørsmål?", "Tredje svar"),
            )

        val vurdertVilkår =
            etVurdertVilkår(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("OPPTJENING"),
                underspørsmål = underspørsmål,
                utfall = VurdertVilkår.Utfall.OPPFYLT,
                notat = "Notat med underspørsmål",
            )

        // when
        repository.lagre(vurdertVilkår)

        // then
        val funnet = repository.finn(vurdertVilkår.id)
        assertNotNull(funnet)

        assertEquals(3, funnet.vurdering.underspørsmål.size)
        assertEquals(underspørsmål[0].spørsmål, funnet.vurdering.underspørsmål[0].spørsmål)
        assertEquals(underspørsmål[0].svar, funnet.vurdering.underspørsmål[0].svar)
        assertEquals(underspørsmål[1].spørsmål, funnet.vurdering.underspørsmål[1].spørsmål)
        assertEquals(underspørsmål[1].svar, funnet.vurdering.underspørsmål[1].svar)
        assertEquals(underspørsmål[2].spørsmål, funnet.vurdering.underspørsmål[2].spørsmål)
        assertEquals(underspørsmål[2].svar, funnet.vurdering.underspørsmål[2].svar)
    }

    @Test
    fun `lagre vilkårsvurdering uten notat`() {
        // given
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår =
            etVurdertVilkår(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("OPPTJENING"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.SKAL_IKKE_VURDERES,
                notat = null,
            )

        // when
        repository.lagre(vurdertVilkår)

        // then
        val funnet = repository.finn(vurdertVilkår.id)
        assertNotNull(funnet)
        assertNull(funnet.vurdering.notat)
        assertEquals(VurdertVilkår.Utfall.SKAL_IKKE_VURDERES, funnet.vurdering.utfall)
    }

    @Test
    fun `lagre flere vilkårsvurderinger for samme behandling`() {
        // given
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår1 =
            etVurdertVilkår(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("OPPTJENING"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.OPPFYLT,
                notat = "Opptjeningsvilkår oppfylt",
            )

        val vurdertVilkår2 =
            etVurdertVilkår(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("SYK_INAKTIV"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                notat = "Syk og inaktiv ikke oppfylt",
            )

        // when
        repository.lagre(vurdertVilkår1)
        repository.lagre(vurdertVilkår2)

        // then
        val funnet1 = repository.finn(vurdertVilkår1.id)
        val funnet2 = repository.finn(vurdertVilkår2.id)

        assertNotNull(funnet1)
        assertNotNull(funnet2)

        assertEquals(Vilkårskode("OPPTJENING"), funnet1.id.vilkårskode)
        assertEquals(VurdertVilkår.Utfall.OPPFYLT, funnet1.vurdering.utfall)

        assertEquals(Vilkårskode("SYK_INAKTIV"), funnet2.id.vilkårskode)
        assertEquals(VurdertVilkår.Utfall.IKKE_OPPFYLT, funnet2.vurdering.utfall)
    }

    @Test
    fun `slett eksisterende vilkårsvurdering`() {
        // given
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår = etVurdertVilkår(behandlingId = behandling.id)
        repository.lagre(vurdertVilkår)

        // verify it exists
        assertNotNull(repository.finn(vurdertVilkår.id))

        // when
        repository.slett(vurdertVilkår.id)

        // then
        val funnet = repository.finn(vurdertVilkår.id)
        assertNull(funnet)
    }

    @Test
    fun `slett en vilkårsvurdering påvirker ikke andre vilkårsvurderinger for samme behandling`() {
        // given
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår1 =
            etVurdertVilkår(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("OPPTJENING"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.OPPFYLT,
                notat = "Opptjeningsvilkår oppfylt",
            )

        val vurdertVilkår2 =
            etVurdertVilkår(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("SYK_INAKTIV"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                notat = "Syk og inaktiv ikke oppfylt",
            )

        repository.lagre(vurdertVilkår1)
        repository.lagre(vurdertVilkår2)

        // when
        repository.slett(vurdertVilkår1.id)

        // then
        assertNull(repository.finn(vurdertVilkår1.id))

        val funnet2 = repository.finn(vurdertVilkår2.id)
        assertNotNull(funnet2)
        assertEquals(Vilkårskode("SYK_INAKTIV"), funnet2.id.vilkårskode)
        assertEquals(VurdertVilkår.Utfall.IKKE_OPPFYLT, funnet2.vurdering.utfall)
    }

    @Test
    fun `hentAlle returnerer alle vilkårsvurderinger for en behandling`() {
        // given
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår1 =
            etVurdertVilkår(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("OPPTJENING"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.OPPFYLT,
                notat = "Opptjeningsvilkår oppfylt",
            )

        val vurdertVilkår2 =
            etVurdertVilkår(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("SYK_INAKTIV"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                notat = "Syk og inaktiv ikke oppfylt",
            )

        val vurdertVilkår3 =
            etVurdertVilkår(
                behandlingId = behandling.id,
                vilkårskode = Vilkårskode("MEDLEMSKAP"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.SKAL_IKKE_VURDERES,
                notat = null,
            )

        repository.lagre(vurdertVilkår1)
        repository.lagre(vurdertVilkår2)
        repository.lagre(vurdertVilkår3)

        // when
        val alleVurderinger = repository.hentAlle(behandling.id)

        // then
        assertEquals(3, alleVurderinger.size)

        val opptjening = alleVurderinger.find { it.id.vilkårskode == Vilkårskode("OPPTJENING") }
        assertNotNull(opptjening)
        assertEquals(VurdertVilkår.Utfall.OPPFYLT, opptjening.vurdering.utfall)
        assertEquals("Opptjeningsvilkår oppfylt", opptjening.vurdering.notat)

        val sykInaktiv = alleVurderinger.find { it.id.vilkårskode == Vilkårskode("SYK_INAKTIV") }
        assertNotNull(sykInaktiv)
        assertEquals(VurdertVilkår.Utfall.IKKE_OPPFYLT, sykInaktiv.vurdering.utfall)
        assertEquals("Syk og inaktiv ikke oppfylt", sykInaktiv.vurdering.notat)

        val medlemskap = alleVurderinger.find { it.id.vilkårskode == Vilkårskode("MEDLEMSKAP") }
        assertNotNull(medlemskap)
        assertEquals(VurdertVilkår.Utfall.SKAL_IKKE_VURDERES, medlemskap.vurdering.utfall)
        assertNull(medlemskap.vurdering.notat)
    }

    @Test
    fun `hentAlle returnerer tom liste når ingen vilkårsvurderinger finnes`() {
        // given
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        // when
        val alleVurderinger = repository.hentAlle(behandling.id)

        // then
        assertEquals(0, alleVurderinger.size)
    }

    @Test
    fun `hentAlle returnerer bare vilkårsvurderinger for riktig behandling`() {
        // given
        val behandling1 = enBehandling()
        val behandling2 = enBehandling()
        behandlingRepository.lagre(behandling1)
        behandlingRepository.lagre(behandling2)

        val vurdertVilkår1 =
            etVurdertVilkår(
                behandlingId = behandling1.id,
                vilkårskode = Vilkårskode("OPPTJENING"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.OPPFYLT,
                notat = "Behandling 1",
            )

        val vurdertVilkår2 =
            etVurdertVilkår(
                behandlingId = behandling2.id,
                vilkårskode = Vilkårskode("OPPTJENING"),
                underspørsmål = emptyList(),
                utfall = VurdertVilkår.Utfall.IKKE_OPPFYLT,
                notat = "Behandling 2",
            )

        repository.lagre(vurdertVilkår1)
        repository.lagre(vurdertVilkår2)

        // when
        val vurderingerForBehandling1 = repository.hentAlle(behandling1.id)

        // then
        assertEquals(1, vurderingerForBehandling1.size)
        assertEquals(behandling1.id, vurderingerForBehandling1[0].id.behandlingId)
        assertEquals("Behandling 1", vurderingerForBehandling1[0].vurdering.notat)
    }
}
