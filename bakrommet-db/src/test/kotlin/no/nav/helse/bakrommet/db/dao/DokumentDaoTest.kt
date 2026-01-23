package no.nav.helse.bakrommet.db.dao

import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.enNavIdent
import no.nav.helse.bakrommet.testutils.tidsstuttet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class DokumentDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource
    val naturligIdent = enNaturligIdent()
    val pseudoId = UUID.nameUUIDFromBytes(naturligIdent.value.toByteArray())
    val saksbehandler = Bruker("ABC", enNavIdent(), "Saksbehandersen@nav.no", roller = emptySet())
    val periode =
        BehandlingDbRecord(
            id = UUID.randomUUID(),
            naturligIdent = naturligIdent,
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
        dao.opprettPseudoId(pseudoId, naturligIdent)
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
