package no.nav.helse.bakrommet.person

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.test.assertEquals

internal class PersonDaoTest {
    val db = MedDataSource(TestDataSource.dbModule.dataSource)
    private val personDao = PersonDaoPg(TestDataSource.dbModule.dataSource)

    @Test
    fun `returnerer null for ukjent person-ID`() {
        assertNull(runBlocking { personDao.finnPersonId("007") })
    }

    @Test
    fun `returnerer spillerom-ID for kjent person-ID`() {
        val fnr = "12121299999"
        opprettTestdata(fnr, "123az")
        assertEquals("123az", runBlocking { personDao.finnPersonId(fnr) })
    }

    @Test
    fun `finner person så lenge en av ID-ene passer`() {
        val fnr = "12121277777"
        opprettTestdata(fnr, "123ab")
        assertEquals("123ab", runBlocking { personDao.finnPersonId("en annen id", fnr) })
    }

    @Test
    fun `kan inserte personer`() {
        val fnr = "12121255555"
        val spilleromId = "987ab"
        runBlocking { personDao.opprettPerson(naturligIdent = fnr, spilleromId = spilleromId) }
        val (identFraDb, spilleromIdFraDb) =
            db.single(
                """select naturlig_ident, spillerom_id from ident where spillerom_id = :sid""",
                "sid" to spilleromId,
            ) {
                it.string("naturlig_ident") to it.string("spillerom_id")
            }!!
        assertEquals(fnr, identFraDb)
        assertEquals(spilleromId, spilleromIdFraDb)
    }

    @Test
    fun `kan finne naturlig ident fra spillerom id`() {
        val fnr = "12121255888"
        val spilleromId = "987tr"
        runBlocking { personDao.opprettPerson(naturligIdent = fnr, spilleromId = spilleromId) }
        val naturligIdent = runBlocking { personDao.finnNaturligIdent(spilleromId = spilleromId) }
        assertEquals(naturligIdent, fnr)
    }

    @Test
    fun `ukjent spilleromid returnerer null`() {
        assertNull(runBlocking { personDao.finnNaturligIdent("fgj75") })
    }

    private fun opprettTestdata(
        fnr: String,
        spilleromId: String,
    ) {
        db.update(
            """
            insert into ident (spillerom_id, naturlig_ident)
            values (:spilleromId, :fnr)
            """.trimIndent(),
            "fnr" to fnr,
            "spilleromId" to spilleromId,
        )
    }
}
