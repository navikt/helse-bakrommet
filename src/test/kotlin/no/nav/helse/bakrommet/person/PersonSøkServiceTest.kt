package no.nav.helse.bakrommet.person

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.TestOppsett.userTokenOgBruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.pdl.PdlMock.pdlReply
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.sql.DataSource
import kotlin.test.assertEquals

class PersonSøkServiceTest {
    lateinit var dataSource: DataSource
    lateinit var personDao: PersonDao
    val fnr1 = "01019011111"
    val fnr2 = "01018022222"
    val pdlClient =
        PdlClient(
            configuration = TestOppsett.configuration.pdl,
            oboClient = TestOppsett.oboClient,
            httpClient =
                PdlMock.mockPdl(
                    mapOf(
                        fnr1 to pdlReply(fnr = fnr1, aktorId = "11111111111"),
                        fnr2 to pdlReply(fnr = fnr2, aktorId = "22222222222"),
                    ),
                ),
        )

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        dataSource = TestDataSource.dbModule.dataSource
        personDao = PersonDao(dataSource)
    }

    private fun serviceMed(antallPåfølgendeKollisjoner: Int) =
        PersonsøkService(
            pdlClient,
            personDao,
            personIdFactory =
                object : PersonIdFactory {
                    var count = 0

                    override fun lagNy() =
                        SpilleromPersonId(
                            personId = if (count++ <= antallPåfølgendeKollisjoner) "aaaaa" else "bbbbb",
                        )
                },
        )

    @Test
    fun `skal takle inntil 3 kollisjoner ved opprettelse av spillerom-personId`() {
        val service = serviceMed(antallPåfølgendeKollisjoner = 3)

        val spilleromId1 = runBlocking { service.hentEllerOpprettPersonid(fnr1, saksbehandler = userTokenOgBruker) }
        assertEquals("aaaaa", spilleromId1.personId)

        val spilleromId2 = runBlocking { service.hentEllerOpprettPersonid(fnr2, saksbehandler = userTokenOgBruker) }
        assertEquals("bbbbb", spilleromId2.personId)
    }

    @Test
    fun `skal ikke takle 4 kollisjoner på rad ved opprettelse av spillerom-personId`() {
        val service = serviceMed(antallPåfølgendeKollisjoner = 4)

        val spilleromId1 = runBlocking { service.hentEllerOpprettPersonid(fnr1, saksbehandler = userTokenOgBruker) }
        assertEquals("aaaaa", spilleromId1.personId)

        assertThrows<RuntimeException> {
            runBlocking { service.hentEllerOpprettPersonid(fnr2, saksbehandler = userTokenOgBruker) }
        }
    }
}
