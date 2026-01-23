package no.nav.helse.bakrommet.db.repository

import kotliquery.sessionOf
import no.nav.helse.bakrommet.assertOffsetDateTimeEquals
import no.nav.helse.bakrommet.db.DBTestFixture
import no.nav.helse.bakrommet.domain.enBehandling
import no.nav.helse.bakrommet.domain.enTilkommenInntekt
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektYrkesaktivitetType
import no.nav.helse.januar
import org.junit.jupiter.api.AfterEach
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.*

class PgTilkommenInntektRepositoryTest {
    private val dataSource = DBTestFixture.module.dataSource
    private val session = sessionOf(dataSource)
    private val repository = PgTilkommenInntektRepository(session)
    private val behandlingRepository = PgBehandlingRepository(session)

    @AfterEach
    fun tearDown() {
        session.close()
    }

    @Test
    fun `lagre og finn tilkommen inntekt`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val tilkommenInntekt = enTilkommenInntekt(behandlingId = behandling.id)
        repository.lagre(tilkommenInntekt)

        val funnet = repository.finn(tilkommenInntekt.id)
        assertNotNull(funnet)

        assertEquals(tilkommenInntekt.id, funnet.id)
        assertEquals(tilkommenInntekt.behandlingId, funnet.behandlingId)
        assertEquals(tilkommenInntekt.ident, funnet.ident)
        assertEquals(tilkommenInntekt.yrkesaktivitetType, funnet.yrkesaktivitetType)
        assertEquals(tilkommenInntekt.fom, funnet.fom)
        assertEquals(tilkommenInntekt.tom, funnet.tom)
        assertEquals(tilkommenInntekt.inntektForPerioden, funnet.inntektForPerioden)
        assertEquals(tilkommenInntekt.notatTilBeslutter, funnet.notatTilBeslutter)
        assertEquals(tilkommenInntekt.ekskluderteDager, funnet.ekskluderteDager)
        assertOffsetDateTimeEquals(tilkommenInntekt.opprettet, funnet.opprettet)
        assertEquals(tilkommenInntekt.opprettetAvNavIdent, funnet.opprettetAvNavIdent)
    }

    @Test
    fun `oppdater tilkommen inntekt`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val tilkommenInntekt = enTilkommenInntekt(behandlingId = behandling.id)
        repository.lagre(tilkommenInntekt)

        val oppdatertTilkommenInntekt =
            enTilkommenInntekt(
                id = tilkommenInntekt.id,
                behandlingId = tilkommenInntekt.behandlingId,
                ident = etOrganisasjonsnummer(),
                yrkesaktivitetType = TilkommenInntektYrkesaktivitetType.PRIVATPERSON,
                fom = 15.januar(2019),
                tom = 20.januar(2019),
                inntektForPerioden = BigDecimal.valueOf(15000),
                notatTilBeslutter = "Oppdatert notat",
                ekskluderteDager = listOf(16.januar(2019)),
                opprettet = tilkommenInntekt.opprettet,
                opprettetAvNavIdent = tilkommenInntekt.opprettetAvNavIdent,
            )

        repository.lagre(oppdatertTilkommenInntekt)

        val funnet = repository.finn(tilkommenInntekt.id)
        assertNotNull(funnet)

        assertEquals(tilkommenInntekt.id, funnet.id)
        assertEquals(tilkommenInntekt.behandlingId, funnet.behandlingId)
        assertEquals(oppdatertTilkommenInntekt.ident, funnet.ident)
        assertEquals(oppdatertTilkommenInntekt.yrkesaktivitetType, funnet.yrkesaktivitetType)
        assertEquals(oppdatertTilkommenInntekt.fom, funnet.fom)
        assertEquals(oppdatertTilkommenInntekt.tom, funnet.tom)
        assertEquals(oppdatertTilkommenInntekt.inntektForPerioden, funnet.inntektForPerioden)
        assertEquals(oppdatertTilkommenInntekt.notatTilBeslutter, funnet.notatTilBeslutter)
        assertEquals(oppdatertTilkommenInntekt.ekskluderteDager, funnet.ekskluderteDager)
        assertOffsetDateTimeEquals(tilkommenInntekt.opprettet, funnet.opprettet)
        assertEquals(tilkommenInntekt.opprettetAvNavIdent, funnet.opprettetAvNavIdent)
    }

    @Test
    fun `finn returnerer null når tilkommen inntekt ikke finnes`() {
        val tilkommenInntekt = enTilkommenInntekt()
        val funnet = repository.finn(tilkommenInntekt.id)
        assertNull(funnet)
    }

    @Test
    fun `slett tilkommen inntekt`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val tilkommenInntekt = enTilkommenInntekt(behandlingId = behandling.id)
        repository.lagre(tilkommenInntekt)

        repository.slett(tilkommenInntekt.id)

        val funnet = repository.finn(tilkommenInntekt.id)
        assertNull(funnet)
    }

    @Test
    fun `finnFor returnerer alle tilkomne inntekter for behandling`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val tilkommenInntekt1 = enTilkommenInntekt(behandlingId = behandling.id)
        val tilkommenInntekt2 = enTilkommenInntekt(behandlingId = behandling.id)
        repository.lagre(tilkommenInntekt1)
        repository.lagre(tilkommenInntekt2)

        val funnet = repository.finnFor(behandling.id)
        assertEquals(2, funnet.size)
        assertTrue(funnet.any { it.id == tilkommenInntekt1.id })
        assertTrue(funnet.any { it.id == tilkommenInntekt2.id })
    }

    @Test
    fun `finnFor returnerer tom liste når ingen tilkomne inntekter finnes`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val funnet = repository.finnFor(behandling.id)
        assertTrue(funnet.isEmpty())
    }

    @Test
    fun `finnFor returnerer kun tilkomne inntekter for riktig behandling`() {
        val behandling1 = enBehandling()
        val behandling2 = enBehandling()
        behandlingRepository.lagre(behandling1)
        behandlingRepository.lagre(behandling2)

        val tilkommenInntekt1 = enTilkommenInntekt(behandlingId = behandling1.id)
        val tilkommenInntekt2 = enTilkommenInntekt(behandlingId = behandling2.id)
        repository.lagre(tilkommenInntekt1)
        repository.lagre(tilkommenInntekt2)

        val funnetForBehandling1 = repository.finnFor(behandling1.id)
        assertEquals(1, funnetForBehandling1.size)
        assertEquals(tilkommenInntekt1.id, funnetForBehandling1.first().id)

        val funnetForBehandling2 = repository.finnFor(behandling2.id)
        assertEquals(1, funnetForBehandling2.size)
        assertEquals(tilkommenInntekt2.id, funnetForBehandling2.first().id)
    }

    @Test
    fun `lagre med yrkesaktivitettype VIRKSOMHET`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val tilkommenInntekt =
            enTilkommenInntekt(
                behandlingId = behandling.id,
                yrkesaktivitetType = TilkommenInntektYrkesaktivitetType.VIRKSOMHET,
            )
        repository.lagre(tilkommenInntekt)

        val funnet = repository.finn(tilkommenInntekt.id)
        assertNotNull(funnet)
        assertEquals(TilkommenInntektYrkesaktivitetType.VIRKSOMHET, funnet.yrkesaktivitetType)
    }

    @Test
    fun `lagre med yrkesaktivitettype PRIVATPERSON`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val tilkommenInntekt =
            enTilkommenInntekt(
                behandlingId = behandling.id,
                yrkesaktivitetType = TilkommenInntektYrkesaktivitetType.PRIVATPERSON,
            )
        repository.lagre(tilkommenInntekt)

        val funnet = repository.finn(tilkommenInntekt.id)
        assertNotNull(funnet)
        assertEquals(TilkommenInntektYrkesaktivitetType.PRIVATPERSON, funnet.yrkesaktivitetType)
    }

    @Test
    fun `lagre med yrkesaktivitettype NÆRINGSDRIVENDE`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val tilkommenInntekt =
            enTilkommenInntekt(
                behandlingId = behandling.id,
                yrkesaktivitetType = TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE,
            )
        repository.lagre(tilkommenInntekt)

        val funnet = repository.finn(tilkommenInntekt.id)
        assertNotNull(funnet)
        assertEquals(TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE, funnet.yrkesaktivitetType)
    }

    @Test
    fun `lagre med ekskluderte dager`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val ekskluderteDager =
            listOf(
                LocalDate.of(2019, 1, 15),
                LocalDate.of(2019, 1, 16),
                LocalDate.of(2019, 1, 20),
            )

        val tilkommenInntekt =
            enTilkommenInntekt(
                behandlingId = behandling.id,
                ekskluderteDager = ekskluderteDager,
            )
        repository.lagre(tilkommenInntekt)

        val funnet = repository.finn(tilkommenInntekt.id)
        assertNotNull(funnet)
        assertEquals(ekskluderteDager.size, funnet.ekskluderteDager.size)
        assertTrue(funnet.ekskluderteDager.containsAll(ekskluderteDager))
    }

    @Test
    fun `lagre med tom liste ekskluderte dager`() {
        val behandling = enBehandling()
        behandlingRepository.lagre(behandling)

        val tilkommenInntekt =
            enTilkommenInntekt(
                behandlingId = behandling.id,
                ekskluderteDager = emptyList(),
            )
        repository.lagre(tilkommenInntekt)

        val funnet = repository.finn(tilkommenInntekt.id)
        assertNotNull(funnet)
        assertTrue(funnet.ekskluderteDager.isEmpty())
    }
}
