package no.nav.helse.bakrommet.db.repository

import kotliquery.sessionOf
import no.nav.helse.bakrommet.assertInstantEquals
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.domain.enBehandling
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus
import no.nav.helse.januar
import org.junit.jupiter.api.AfterEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PgBehandlingRepositoryTest {
    private val dataSource = TestDataSource.dbModule.dataSource
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
                beslutterNavIdent = "Z222222",
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
                beslutterNavIdent = "Z222222",
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
}
