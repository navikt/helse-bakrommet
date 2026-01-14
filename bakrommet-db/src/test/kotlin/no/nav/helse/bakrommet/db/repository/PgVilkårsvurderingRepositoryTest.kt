package no.nav.helse.bakrommet.db.repository

import kotliquery.sessionOf
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.januar
import java.time.Instant
import java.util.*
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
        val behandling = behandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår = vurdertVilkår(behandling.id)

        // when
        repository.lagre(vurdertVilkår)

        // then
        val funnet = repository.finn(vurdertVilkår.id)
        assertNotNull(funnet)

        assertEquals(vurdertVilkår.id, funnet.id)
        assertEquals(vurdertVilkår.vurdering, funnet.vurdering)
        assertEquals(vurdertVilkår.hovedspørsmål, funnet.hovedspørsmål)
        assertEquals(vurdertVilkår.underspørsmål, funnet.underspørsmål)
        assertEquals(vurdertVilkår.notat, funnet.notat)
    }

    @Test
    fun `finn returnerer null når vilkårsvurdering ikke finnes`() {
        val behandling = behandling()
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
        val behandling = behandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår = vurdertVilkår(behandling.id)
        repository.lagre(vurdertVilkår)

        // when
        vurdertVilkår.nyVurdering(
            vurdering = VurdertVilkår.Vurdering.IKKE_OPPFYLT,
            notat = "Oppdatert notat",
        )
        repository.lagre(vurdertVilkår)

        // then
        val funnet = repository.finn(vurdertVilkår.id)
        assertNotNull(funnet)

        assertEquals(vurdertVilkår.id, funnet.id)
        assertEquals(VurdertVilkår.Vurdering.IKKE_OPPFYLT, funnet.vurdering)
        assertEquals("Oppdatert notat", funnet.notat)
        assertEquals(vurdertVilkår.hovedspørsmål, funnet.hovedspørsmål)
        assertEquals(vurdertVilkår.underspørsmål, funnet.underspørsmål)
    }

    @Test
    fun `lagre vilkårsvurdering med underspørsmål`() {
        // given
        val behandling = behandling()
        behandlingRepository.lagre(behandling)

        val underspørsmål =
            listOf(
                VilkårsvurderingUnderspørsmål("Første spørsmål?", "Første svar"),
                VilkårsvurderingUnderspørsmål("Andre spørsmål?", "Andre svar"),
                VilkårsvurderingUnderspørsmål("Tredje spørsmål?", "Tredje svar"),
            )

        val vurdertVilkår =
            VurdertVilkår(
                id =
                    VilkårsvurderingId(
                        behandlingId = behandling.id,
                        vilkårskode = Vilkårskode("OPPTJENING"),
                    ),
                hovedspørsmål = "Har bruker oppfylt opptjeningsvilkåret?",
                underspørsmål = underspørsmål,
                vurdering = VurdertVilkår.Vurdering.OPPFYLT,
                notat = "Notat med underspørsmål",
            )

        // when
        repository.lagre(vurdertVilkår)

        // then
        val funnet = repository.finn(vurdertVilkår.id)
        assertNotNull(funnet)

        assertEquals(3, funnet.underspørsmål.size)
        assertEquals(underspørsmål[0].spørsmål, funnet.underspørsmål[0].spørsmål)
        assertEquals(underspørsmål[0].svar, funnet.underspørsmål[0].svar)
        assertEquals(underspørsmål[1].spørsmål, funnet.underspørsmål[1].spørsmål)
        assertEquals(underspørsmål[1].svar, funnet.underspørsmål[1].svar)
        assertEquals(underspørsmål[2].spørsmål, funnet.underspørsmål[2].spørsmål)
        assertEquals(underspørsmål[2].svar, funnet.underspørsmål[2].svar)
    }

    @Test
    fun `lagre vilkårsvurdering uten notat`() {
        // given
        val behandling = behandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår =
            VurdertVilkår(
                id =
                    VilkårsvurderingId(
                        behandlingId = behandling.id,
                        vilkårskode = Vilkårskode("OPPTJENING"),
                    ),
                hovedspørsmål = "Har bruker oppfylt opptjeningsvilkåret?",
                underspørsmål = emptyList(),
                vurdering = VurdertVilkår.Vurdering.SKAL_IKKE_VURDERES,
                notat = null,
            )

        // when
        repository.lagre(vurdertVilkår)

        // then
        val funnet = repository.finn(vurdertVilkår.id)
        assertNotNull(funnet)
        assertNull(funnet.notat)
        assertEquals(VurdertVilkår.Vurdering.SKAL_IKKE_VURDERES, funnet.vurdering)
    }

    @Test
    fun `lagre flere vilkårsvurderinger for samme behandling`() {
        // given
        val behandling = behandling()
        behandlingRepository.lagre(behandling)

        val vurdertVilkår1 =
            VurdertVilkår(
                id =
                    VilkårsvurderingId(
                        behandlingId = behandling.id,
                        vilkårskode = Vilkårskode("OPPTJENING"),
                    ),
                hovedspørsmål = "Har bruker oppfylt opptjeningsvilkåret?",
                underspørsmål = emptyList(),
                vurdering = VurdertVilkår.Vurdering.OPPFYLT,
                notat = "Opptjeningsvilkår oppfylt",
            )

        val vurdertVilkår2 =
            VurdertVilkår(
                id =
                    VilkårsvurderingId(
                        behandlingId = behandling.id,
                        vilkårskode = Vilkårskode("SYK_INAKTIV"),
                    ),
                hovedspørsmål = "Er bruker syk og inaktiv?",
                underspørsmål = emptyList(),
                vurdering = VurdertVilkår.Vurdering.IKKE_OPPFYLT,
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
        assertEquals(VurdertVilkår.Vurdering.OPPFYLT, funnet1.vurdering)

        assertEquals(Vilkårskode("SYK_INAKTIV"), funnet2.id.vilkårskode)
        assertEquals(VurdertVilkår.Vurdering.IKKE_OPPFYLT, funnet2.vurdering)
    }

    private fun behandling(): Behandling =
        Behandling.fraLagring(
            id = BehandlingId(UUID.randomUUID()),
            naturligIdent = NaturligIdent("12345678910"),
            opprettet = Instant.now(),
            opprettetAvNavIdent = "Z999999",
            opprettetAvNavn = "En Saksbehandler",
            fom = 1.januar(2018),
            tom = 31.januar(2018),
            status = BehandlingStatus.UNDER_BEHANDLING,
            beslutterNavIdent = "Z999999",
            skjæringstidspunkt = 1.januar(2018),
            individuellBegrunnelse = "En begrunnelse",
            sykepengegrunnlagId = null,
            revurdertAvBehandlingId = null,
            revurdererSaksbehandlingsperiodeId = null,
        )

    private fun vurdertVilkår(behandlingId: BehandlingId): VurdertVilkår =
        VurdertVilkår(
            id =
                VilkårsvurderingId(
                    behandlingId = behandlingId,
                    vilkårskode = Vilkårskode("OPPTJENING"),
                ),
            hovedspørsmål = "Har bruker oppfylt opptjeningsvilkåret?",
            underspørsmål =
                listOf(
                    VilkårsvurderingUnderspørsmål("Har bruker jobbet 4 uker?", "Ja"),
                    VilkårsvurderingUnderspørsmål("Er det tilstrekkelig opptjening?", "Ja"),
                ),
            vurdering = VurdertVilkår.Vurdering.OPPFYLT,
            notat = "Dette er et notat",
        )
}
