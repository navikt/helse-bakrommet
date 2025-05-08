package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.util.insert
import no.nav.helse.bakrommet.util.single
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.test.assertEquals

internal class PersonDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource
    private val personDao = PersonDao(dataSource)

    @Test
    fun `returnerer null for ukjent person-ID`() {
        assertNull(personDao.finnPersonId("007"))
    }

    @Test
    fun `returnerer spillerom-ID for kjent person-ID`() {
        val fnr = "12121299999"
        opprettTestdata(fnr, "123az")
        assertEquals("123az", personDao.finnPersonId(fnr))
    }

    @Test
    fun `finner person så lenge en av ID-ene passer`() {
        val fnr = "12121277777"
        opprettTestdata(fnr, "123ab")
        assertEquals("123ab", personDao.finnPersonId("en annen id", fnr))
    }

    @Test
    fun `kan inserte personer`() {
        val fnr = "12121255555"
        val spilleromId = "987ab"
        personDao.opprettPerson(naturligIdent = fnr, spilleromId = spilleromId)
        val (identFraDb, spilleromIdFraDb) =
            dataSource.single(
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
        personDao.opprettPerson(naturligIdent = fnr, spilleromId = spilleromId)
        val naturligIdent = personDao.finnNaturligIdent(spilleromId = spilleromId)
        assertEquals(naturligIdent, fnr)
    }

    @Test
    fun `ukjent spilleromid returnerer null`() {
        assertNull(personDao.finnNaturligIdent("fgj75"))
    }

    private fun opprettTestdata(
        fnr: String,
        spilleromId: String,
    ) {
        dataSource.insert(
            """
            insert into ident (spillerom_id, naturlig_ident)
            values (:spilleromId, :fnr)
            """.trimIndent(),
            "fnr" to fnr,
            "spilleromId" to spilleromId,
        )
    }
}
