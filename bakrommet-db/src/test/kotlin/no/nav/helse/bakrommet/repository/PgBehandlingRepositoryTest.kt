package no.nav.helse.bakrommet.repository

import kotliquery.sessionOf
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.db.repository.PgBehandlingRepository
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus
import no.nav.helse.januar
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgBehandlingRepositoryTest {
    private val dataSource = TestDataSource.dbModule.dataSource
    private val session = sessionOf(dataSource)
    private val repository = PgBehandlingRepository(session)

    @Test
    fun `lagre og finn`() {
        val behandling = behandling()
        repository.lagre(behandling)

        val funnet = repository.finn(behandling.id)
        assertNotNull(funnet)

        assertEquals(behandling.id, funnet.id)
        assertEquals(behandling.naturligIdent, funnet.naturligIdent)
        assertEquals(behandling.opprettet, funnet.opprettet)
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
        assertEquals(behandling.revurdererSaksbehandlingsperiodeId, funnet.revurdererSaksbehandlingsperiodeId)
    }

    @Test
    fun oppdater() {
        // given
        val behandling = behandling()
        repository.lagre(behandling)

        val oppdatertBehandling =
            Behandling.fraLagring(
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
        assertEquals(behandling.opprettet, funnet.opprettet)
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
        assertEquals(oppdatertBehandling.revurdererSaksbehandlingsperiodeId, funnet.revurdererSaksbehandlingsperiodeId)
    }

    @Test
    fun `lagre referanse til annen behandling`() {
        // given
        val behandling = behandling()
        repository.lagre(behandling)

        val oppdatertBehandling =
            Behandling.fraLagring(
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
        assertEquals(behandling.opprettet, funnet.opprettet)
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
        assertEquals(oppdatertBehandling.revurdererSaksbehandlingsperiodeId, funnet.revurdererSaksbehandlingsperiodeId)
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
}
