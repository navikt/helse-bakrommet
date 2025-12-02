package no.nav.helse.bakrommet.behandling.tilkommen

import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.BehandlingDaoPg
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDaoPg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class TilkommenInntektDaoTest {
    private val dataSource = TestDataSource.dbModule.dataSource
    private val personDao = PersonDaoPg(dataSource)
    private val behandlingDao = BehandlingDaoPg(dataSource)
    private val dao = TilkommenInntektDaoPg(dataSource)

    private val spilleromId = "spiller-1"
    private val naturligIdent = "01019012345"
    private val behandlingId = UUID.randomUUID()
    private val saksbehandlerNavn = "Zara Saksbehandler"
    private val saksbehandlerIdent = "Z12345"

    @BeforeEach
    fun setUp() {
        TestDataSource.resetDatasource()
        personDao.opprettPerson(naturligIdent, spilleromId)
        behandlingDao.opprettPeriode(
            Behandling(
                id = behandlingId,
                spilleromPersonId = spilleromId,
                opprettet = OffsetDateTime.now(),
                opprettetAvNavIdent = saksbehandlerIdent,
                opprettetAvNavn = saksbehandlerNavn,
                fom = LocalDate.of(2025, 6, 9),
                tom = LocalDate.of(2025, 6, 18),
                skjæringstidspunkt = LocalDate.of(2025, 6, 9),
            ),
        )
    }

    @Test
    fun `lagrer og henter tilkommen inntekt for behandling`() {
        val lagret = dao.opprett(nyTilkommenInntekt())

        val funnet = dao.hentForBehandling(behandlingId)

        assertEquals(1, funnet.size)
        val hentet = funnet.first()
        assertEquals(lagret.id, hentet.id)
        assertEquals(TilkommenInntektYrkesaktivitetType.VIRKSOMHET, hentet.tilkommenInntekt.yrkesaktivitetType)
        assertEquals(BigDecimal("3330.00"), hentet.tilkommenInntekt.inntektForPerioden)
        assertEquals(LocalDate.of(2025, 6, 9), hentet.tilkommenInntekt.ekskluderteDager.first())
    }

    @Test
    fun `oppdaterer eksisterende tilkommen inntekt`() {
        val eksisterende = dao.opprett(nyTilkommenInntekt())
        val oppdatert =
            dao.oppdater(
                id = eksisterende.id,
                tilkommenInntekt =
                    eksisterende.tilkommenInntekt.copy(
                        inntektForPerioden = BigDecimal("4000.00"),
                        notatTilBeslutter = "Oppdatert notat",
                    ),
            )

        assertEquals(BigDecimal("4000.00"), oppdatert.tilkommenInntekt.inntektForPerioden)
        assertEquals("Oppdatert notat", oppdatert.tilkommenInntekt.notatTilBeslutter)
        assertEquals(eksisterende.opprettet, oppdatert.opprettet)
    }

    @Test
    fun `sletter tilkommen inntekt`() {
        val lagret = dao.opprett(nyTilkommenInntekt())

        dao.slett(behandlingId = lagret.behandlingId, id = lagret.id)

        val funnet = dao.hentForBehandling(behandlingId)
        assertTrue(funnet.isEmpty())
    }

    private fun nyTilkommenInntekt(
        id: UUID = UUID.randomUUID(),
        behandling: UUID = behandlingId,
        opprettet: OffsetDateTime = OffsetDateTime.now(),
    ) = TilkommenInntektDbRecord(
        id = id,
        behandlingId = behandling,
        tilkommenInntekt =
            TilkommenInntekt(
                ident = "967170232",
                yrkesaktivitetType = TilkommenInntektYrkesaktivitetType.VIRKSOMHET,
                fom = LocalDate.of(2025, 6, 9),
                tom = LocalDate.of(2025, 6, 18),
                inntektForPerioden = BigDecimal("3330.00"),
                notatTilBeslutter = "Ønsker at helgen skjermes",
                ekskluderteDager =
                    listOf(
                        LocalDate.of(2025, 6, 9),
                        LocalDate.of(2025, 6, 10),
                    ),
            ),
        opprettet = opprettet,
        opprettetAvNavIdent = saksbehandlerIdent,
    )
}
