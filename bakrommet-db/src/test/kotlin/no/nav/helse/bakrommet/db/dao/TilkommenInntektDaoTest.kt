package no.nav.helse.bakrommet.db.dao

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntekt
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDbRecord
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.NaturligIdent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class TilkommenInntektDaoTest {
    private val dataSource = TestDataSource.dbModule.dataSource
    private val personDao = PersonPseudoIdDaoPg(dataSource)
    private val behandlingDao = BehandlingDaoPg(dataSource)
    private val dao = TilkommenInntektDaoPg(dataSource)

    private val behandlingId = UUID.randomUUID()
    private val saksbehandlerNavn = "Zara Saksbehandler"
    private val saksbehandlerIdent = "Z12345"

    @BeforeEach
    fun setUp() {
        TestDataSource.resetDatasource()
        val fnr = "01019012345"
        val pseudoId = UUID.nameUUIDFromBytes(fnr.toByteArray())
        personDao.opprettPseudoId(pseudoId, NaturligIdent(fnr))
        behandlingDao.opprettPeriode(
            BehandlingDbRecord(
                id = behandlingId,
                naturligIdent = NaturligIdent(fnr),
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

        Assertions.assertEquals(1, funnet.size)
        val hentet = funnet.first()
        Assertions.assertEquals(lagret.id, hentet.id)
        Assertions.assertEquals(
            TilkommenInntektYrkesaktivitetType.VIRKSOMHET,
            hentet.tilkommenInntekt.yrkesaktivitetType,
        )
        Assertions.assertEquals(BigDecimal("3330.00"), hentet.tilkommenInntekt.inntektForPerioden)
        Assertions.assertEquals(LocalDate.of(2025, 6, 9), hentet.tilkommenInntekt.ekskluderteDager.first())
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

        Assertions.assertEquals(BigDecimal("4000.00"), oppdatert.tilkommenInntekt.inntektForPerioden)
        Assertions.assertEquals("Oppdatert notat", oppdatert.tilkommenInntekt.notatTilBeslutter)
        Assertions.assertEquals(eksisterende.opprettet, oppdatert.opprettet)
    }

    @Test
    fun `sletter tilkommen inntekt`() {
        val lagret = dao.opprett(nyTilkommenInntekt())

        dao.slett(behandlingId = lagret.behandlingId, id = lagret.id)

        val funnet = dao.hentForBehandling(behandlingId)
        Assertions.assertTrue(funnet.isEmpty())
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
