package no.nav.helse.bakrommet.db.repository

import kotliquery.sessionOf
import no.nav.helse.bakrommet.assertInstantEquals
import no.nav.helse.bakrommet.db.DBTestFixture
import no.nav.helse.bakrommet.domain.enBehandling
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.enNavIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus
import no.nav.helse.januar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import kotlin.test.*

class PgBehandlingRepositoryTest {
    private val dataSource = DBTestFixture.module.dataSource
    private val session = sessionOf(dataSource)
    private val repository = PgBehandlingRepository(session)

    @AfterEach
    fun tearDown() {
        session.close()
    }

    @Test
    fun `lagre og finn`() {
        val behandling = enBehandling()
        repository.lagre(behandling)

        val funnet = repository.finn(behandling.id)
        assertNotNull(funnet)

        assertEquals(behandling.id, funnet.id)
        assertEquals(behandling.naturligIdent, funnet.naturligIdent)
        assertInstantEquals(behandling.opprettet, funnet.opprettet)
        assertEquals(behandling.opprettetAvNavIdent, funnet.opprettetAvNavIdent)
        assertEquals(behandling.opprettetAvNavn, funnet.opprettetAvNavn)
        assertEquals(behandling.fom, funnet.fom)
        assertEquals(behandling.tom, funnet.tom)
        assertEquals(behandling.status, funnet.status)
        assertEquals(behandling.beslutterNavIdent, funnet.beslutterNavIdent)
        assertEquals(behandling.skjæringstidspunkt, funnet.skjæringstidspunkt)
        assertEquals(behandling.individuellBegrunnelse, funnet.individuellBegrunnelse)
        assertEquals(behandling.sykepengegrunnlagId, funnet.sykepengegrunnlagId)
        assertEquals(behandling.revurdertAvBehandlingId, funnet.revurdertAvBehandlingId)
        assertEquals(behandling.revurdererBehandlingId, funnet.revurdererBehandlingId)
    }

    @Test
    fun oppdater() {
        // given
        val behandling = enBehandling()
        repository.lagre(behandling)

        val oppdatertBehandling =
            enBehandling(
                id = behandling.id,
                naturligIdent = behandling.naturligIdent,
                opprettet = behandling.opprettet,
                opprettetAvNavIdent = behandling.opprettetAvNavIdent,
                opprettetAvNavn = behandling.opprettetAvNavn,
                fom = 1.januar(2019),
                tom = 31.januar(2019),
                skjæringstidspunkt = 1.januar(2019),
                status = BehandlingStatus.TIL_BESLUTNING,
                beslutterNavIdent = enNavIdent(),
                individuellBegrunnelse = "En annen begrunnelse",
                sykepengegrunnlagId = null,
                revurdertAvBehandlingId = null,
                revurdererSaksbehandlingsperiodeId = null,
            )

        // when
        repository.lagre(oppdatertBehandling)

        // then
        val funnet = repository.finn(oppdatertBehandling.id)
        assertNotNull(funnet)

        assertEquals(behandling.id, funnet.id)
        assertEquals(behandling.naturligIdent, funnet.naturligIdent)
        assertInstantEquals(behandling.opprettet, funnet.opprettet)
        assertEquals(behandling.opprettetAvNavIdent, funnet.opprettetAvNavIdent)
        assertEquals(behandling.opprettetAvNavn, funnet.opprettetAvNavn)
        assertEquals(oppdatertBehandling.fom, funnet.fom)
        assertEquals(oppdatertBehandling.tom, funnet.tom)
        assertEquals(oppdatertBehandling.status, funnet.status)
        assertEquals(oppdatertBehandling.beslutterNavIdent, funnet.beslutterNavIdent)
        assertEquals(oppdatertBehandling.skjæringstidspunkt, funnet.skjæringstidspunkt)
        assertEquals(oppdatertBehandling.individuellBegrunnelse, funnet.individuellBegrunnelse)
        assertEquals(oppdatertBehandling.sykepengegrunnlagId, funnet.sykepengegrunnlagId)
        assertEquals(oppdatertBehandling.revurdertAvBehandlingId, funnet.revurdertAvBehandlingId)
        assertEquals(oppdatertBehandling.revurdererBehandlingId, funnet.revurdererBehandlingId)
    }

    @Test
    fun `lagre referanse til annen behandling`() {
        // given
        val behandling = enBehandling()
        repository.lagre(behandling)

        val oppdatertBehandling =
            enBehandling(
                id = behandling.id,
                naturligIdent = behandling.naturligIdent,
                opprettet = behandling.opprettet,
                opprettetAvNavIdent = behandling.opprettetAvNavIdent,
                opprettetAvNavn = behandling.opprettetAvNavn,
                fom = 1.januar(2019),
                tom = 31.januar(2019),
                skjæringstidspunkt = 1.januar(2019),
                status = BehandlingStatus.TIL_BESLUTNING,
                beslutterNavIdent = enNavIdent(),
                individuellBegrunnelse = "En annen begrunnelse",
                sykepengegrunnlagId = null,
                revurdertAvBehandlingId = behandling.id,
                revurdererSaksbehandlingsperiodeId = behandling.id,
            )

        // when
        repository.lagre(oppdatertBehandling)

        // then
        val funnet = repository.finn(oppdatertBehandling.id)
        assertNotNull(funnet)

        assertEquals(behandling.id, funnet.id)
        assertEquals(behandling.naturligIdent, funnet.naturligIdent)
        assertInstantEquals(behandling.opprettet, funnet.opprettet)
        assertEquals(behandling.opprettetAvNavIdent, funnet.opprettetAvNavIdent)
        assertEquals(behandling.opprettetAvNavn, funnet.opprettetAvNavn)
        assertEquals(oppdatertBehandling.fom, funnet.fom)
        assertEquals(oppdatertBehandling.tom, funnet.tom)
        assertEquals(oppdatertBehandling.status, funnet.status)
        assertEquals(oppdatertBehandling.beslutterNavIdent, funnet.beslutterNavIdent)
        assertEquals(oppdatertBehandling.skjæringstidspunkt, funnet.skjæringstidspunkt)
        assertEquals(oppdatertBehandling.individuellBegrunnelse, funnet.individuellBegrunnelse)
        assertEquals(oppdatertBehandling.sykepengegrunnlagId, funnet.sykepengegrunnlagId)
        assertEquals(oppdatertBehandling.revurdertAvBehandlingId, funnet.revurdertAvBehandlingId)
        assertEquals(oppdatertBehandling.revurdererBehandlingId, funnet.revurdererBehandlingId)
    }

    @Test
    fun `henter alle behandlinger for en naturlig ident`() {
        // given
        val naturligIdent = enNaturligIdent()
        val behandling1 = enBehandling(naturligIdent = naturligIdent)
        val behandling2 = enBehandling(naturligIdent = naturligIdent)
        repository.lagre(behandling1)
        repository.lagre(behandling2)
        val enBehandlingForEnAnnenPerson = enBehandling()
        repository.lagre(enBehandlingForEnAnnenPerson)

        // when
        val funnet = repository.finnFor(naturligIdent)

        // then
        assertEquals(2, funnet.size)
        assertTrue(funnet.map { it.id }.containsAll(listOf(behandling1.id, behandling2.id)))
        assertFalse(funnet.map { it.id }.contains(enBehandlingForEnAnnenPerson.id))
    }

    @ParameterizedTest
    @EnumSource(Behandling.Endring.TypeEndring::class)
    fun `lagrer og henter endring med type Startet`(typeEndring: Behandling.Endring.TypeEndring) {
        // given
        val navIdent = enNavIdent()
        val behandling =
            enBehandling(
                endringer =
                    listOf(
                        Behandling.Endring(
                            type = typeEndring,
                            tidspunkt = Instant.now(),
                            navIdent = navIdent,
                            status = BehandlingStatus.UNDER_BEHANDLING,
                            beslutterNavIdent = null,
                            kommentar = null,
                        ),
                    ),
            )

        // when
        repository.lagre(behandling)

        // then
        val funnet = repository.finn(behandling.id)
        assertNotNull(funnet)
        assertEquals(1, funnet.endringer.size)
        val endring = funnet.endringer.first()
        assertEquals(typeEndring, endring.type)
        assertEquals(navIdent, endring.navIdent)
        assertEquals(BehandlingStatus.UNDER_BEHANDLING, endring.status)
        assertEquals(null, endring.beslutterNavIdent)
        assertEquals(null, endring.kommentar)
        assertInstantEquals(behandling.endringer.first().tidspunkt, endring.tidspunkt)
    }

    @Test
    fun `lagrer og henter flere endringer i riktig rekkefølge`() {
        // given
        val tidspunkt1 = Instant.now().minusSeconds(120)
        val tidspunkt2 = Instant.now().minusSeconds(60)
        val tidspunkt3 = Instant.now()
        val navIdent = enNavIdent()
        val behandling =
            enBehandling(
                status = BehandlingStatus.TIL_BESLUTNING,
                endringer =
                    listOf(
                        Behandling.Endring(
                            type = Behandling.Endring.TypeEndring.Startet,
                            tidspunkt = tidspunkt1,
                            navIdent = navIdent,
                            status = BehandlingStatus.UNDER_BEHANDLING,
                            beslutterNavIdent = null,
                            kommentar = null,
                        ),
                        Behandling.Endring(
                            type = Behandling.Endring.TypeEndring.OppdatertIndividuellBegrunnelse,
                            tidspunkt = tidspunkt2,
                            navIdent = navIdent,
                            status = BehandlingStatus.UNDER_BEHANDLING,
                            beslutterNavIdent = null,
                            kommentar = null,
                        ),
                        Behandling.Endring(
                            type = Behandling.Endring.TypeEndring.SendtTilBeslutning,
                            tidspunkt = tidspunkt3,
                            navIdent = navIdent,
                            status = BehandlingStatus.TIL_BESLUTNING,
                            beslutterNavIdent = null,
                            kommentar = "Sendt videre",
                        ),
                    ),
            )

        // when
        repository.lagre(behandling)

        // then
        val funnet = repository.finn(behandling.id)
        assertNotNull(funnet)
        assertEquals(3, funnet.endringer.size)
        assertEquals(Behandling.Endring.TypeEndring.Startet, funnet.endringer[0].type)
        assertEquals(Behandling.Endring.TypeEndring.OppdatertIndividuellBegrunnelse, funnet.endringer[1].type)
        assertEquals(Behandling.Endring.TypeEndring.SendtTilBeslutning, funnet.endringer[2].type)
        assertInstantEquals(tidspunkt1, funnet.endringer[0].tidspunkt)
        assertInstantEquals(tidspunkt2, funnet.endringer[1].tidspunkt)
        assertInstantEquals(tidspunkt3, funnet.endringer[2].tidspunkt)
    }

    @Test
    fun `lagrer kun nye endringer ved oppdatering av behandling`() {
        // given
        val navIdent = enNavIdent()
        val førsteEndring =
            Behandling.Endring(
                type = Behandling.Endring.TypeEndring.Startet,
                tidspunkt = Instant.now().minusSeconds(60),
                navIdent = navIdent,
                status = BehandlingStatus.UNDER_BEHANDLING,
                beslutterNavIdent = null,
                kommentar = null,
            )
        val behandling = enBehandling(endringer = listOf(førsteEndring))
        repository.lagre(behandling)

        val andreEndring =
            Behandling.Endring(
                type = Behandling.Endring.TypeEndring.SendtTilBeslutning,
                tidspunkt = Instant.now(),
                navIdent = navIdent,
                status = BehandlingStatus.TIL_BESLUTNING,
                beslutterNavIdent = null,
                kommentar = null,
            )
        val oppdatertBehandling =
            enBehandling(
                id = behandling.id,
                naturligIdent = behandling.naturligIdent,
                opprettet = behandling.opprettet,
                opprettetAvNavIdent = behandling.opprettetAvNavIdent,
                opprettetAvNavn = behandling.opprettetAvNavn,
                status = BehandlingStatus.TIL_BESLUTNING,
                endringer = listOf(førsteEndring, andreEndring),
            )

        // when
        repository.lagre(oppdatertBehandling)

        // then
        val funnet = repository.finn(behandling.id)
        assertNotNull(funnet)
        assertEquals(2, funnet.endringer.size)
        assertEquals(Behandling.Endring.TypeEndring.Startet, funnet.endringer[0].type)
        assertEquals(Behandling.Endring.TypeEndring.SendtTilBeslutning, funnet.endringer[1].type)
    }
}
