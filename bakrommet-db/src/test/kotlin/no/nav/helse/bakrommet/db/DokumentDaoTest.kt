package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDaoPg
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.person.PersonPseudoIdDaoPg
import no.nav.helse.bakrommet.testutils.tidsstuttet
import no.nav.helse.bakrommet.util.Kildespor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class DokumentDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource
    val fnr = "01019012345"
    val pseudoId = UUID.nameUUIDFromBytes(fnr.toByteArray())
    val saksbehandler = Bruker("ABC", "A. B. C", "Saksbehandersen@nav.no", roller = emptySet())
    val periode =
        BehandlingDbRecord(
            id = UUID.randomUUID(),
            naturligIdent = NaturligIdent(fnr),
            opprettet = OffsetDateTime.now(),
            opprettetAvNavIdent = saksbehandler.navIdent,
            opprettetAvNavn = saksbehandler.navn,
            fom = LocalDate.now().minusMonths(1),
            tom = LocalDate.now().minusDays(1),
            skjæringstidspunkt = LocalDate.now().minusMonths(1),
        )

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        val dao = PersonPseudoIdDaoPg(dataSource)
        dao.opprettPseudoId(pseudoId, NaturligIdent(fnr))
        val behandlingDao = BehandlingDaoPg(dataSource)
        behandlingDao.opprettPeriode(periode)
    }

    @Test
    fun `oppretter og henter et dokument tilknyttet en behandling`() {
        val dao = DokumentDaoPg(dataSource)
        val dok =
            Dokument(
                id = UUID.randomUUID(),
                dokumentType = "ainntekt_828",
                eksternId = "søknad-1",
                innhold = """{ "inntekter" : [] "}""",
                opprettet = Instant.now(),
                sporing = Kildespor("GET /søknader/søknad-1"),
                opprettetForBehandling = periode.id,
            )
        dao.opprettDokument(dok)

        Assertions.assertEquals(listOf(dok).tidsstuttet(), dao.hentDokumenterFor(periode.id).tidsstuttet())

        Assertions.assertEquals(dok.tidsstuttet(), dao.hentDokument(dok.id)?.tidsstuttet())
    }
}
