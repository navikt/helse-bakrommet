package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import no.nav.helse.bakrommet.testutils.tidsstuttet
import no.nav.helse.bakrommet.util.Kildespor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class DokumentDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource
    val fnr = "01019012345"
    val personId = "0h0a1"
    val saksbehandler = Bruker("ABC", "A. B. C", "Saksbehandersen@nav.no", roller = emptySet())
    val periode =
        Saksbehandlingsperiode(
            id = UUID.randomUUID(),
            spilleromPersonId = personId,
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
        val dao = PersonDao(dataSource)
        dao.opprettPerson(fnr, personId)
        val behandlingDao = SaksbehandlingsperiodeDao(dataSource)
        behandlingDao.opprettPeriode(periode)
    }

    @Test
    fun `oppretter og henter et dokument tilknyttet en behandling`() {
        val dao = DokumentDao(dataSource)
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

        assertEquals(listOf(dok).tidsstuttet(), dao.hentDokumenterFor(periode.id).tidsstuttet())

        assertEquals(dok.tidsstuttet(), dao.hentDokument(dok.id)?.tidsstuttet())
    }
}
