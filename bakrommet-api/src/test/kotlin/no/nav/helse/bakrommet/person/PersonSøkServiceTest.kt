package no.nav.helse.bakrommet.person

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.auth.Rolle
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.pdl.PdlMock.pdlReply
import no.nav.helse.bakrommet.skapDbDaoer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PersonSøkServiceTest {
    lateinit var db: DbDaoer<PersonsokDaoer>
    val fnr1 = "01019011111"
    val fnr2 = "01018022222"
    val aktor1 = "1111111111111"
    val aktor2 = "2222222222222"
    val pdlClient =
        PdlMock.pdlClient(
            identTilReplyMap =
                mapOf(
                    fnr1 to pdlReply(fnr = fnr1, aktorId = aktor1),
                    fnr2 to pdlReply(fnr = fnr2, aktorId = aktor2),
                    aktor1 to pdlReply(fnr = fnr1, aktorId = aktor1),
                    aktor2 to pdlReply(fnr = fnr2, aktorId = aktor2),
                ),
        )
    val userTokenOgBruker =
        BrukerOgToken(
            token = SpilleromBearerToken("token"),
            bruker =
                Bruker(
                    navn = "Saksbehandler Navn",
                    navIdent = "Z123456",
                    preferredUsername = "saksbehandler",
                    roller = setOf(Rolle.SAKSBEHANDLER),
                ),
        )

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        db = skapDbDaoer(TestDataSource.dbModule.dataSource)
    }

    private fun serviceMed(antallPåfølgendeKollisjoner: Int) =
        PersonsøkService(
            pdlClient,
            db,
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

    private fun service() =
        PersonsøkService(
            pdlClient,
            db,
            personIdFactory =
                object : PersonIdFactory {
                    var count = 0
                    val chars = ('a'..'z').toList()

                    override fun lagNy() =
                        SpilleromPersonId(
                            personId = chars[count].toString().repeat(5),
                        )
                },
        )

    @Test
    fun `ved hentEllerOpprett() på AktørID så er det Folkeregisterident som skal kobles til personId`() {
        val service = service()
        val spilleromId1 = runBlocking { service.hentEllerOpprettPersonid(aktor1, saksbehandler = userTokenOgBruker) }
        val actualPersonId = runBlocking { db.nonTransactional { personDao.finnPersonId(fnr1) } }

        assertEquals(spilleromId1.personId, actualPersonId)
    }
}
