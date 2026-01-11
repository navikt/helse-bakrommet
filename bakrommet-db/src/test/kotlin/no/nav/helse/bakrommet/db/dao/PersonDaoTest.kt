package no.nav.helse.bakrommet.db.dao

import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.person.somNaturligIdent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.util.UUID
import kotlin.test.assertEquals

internal class PersonDaoTest {
    val db = MedDataSource(TestDataSource.dbModule.dataSource)
    private val personDao = PersonPseudoIdDaoPg(TestDataSource.dbModule.dataSource)

    @Test
    fun `returnerer spillerom-ID for kjent person-ID`() {
        val fnr = "12121299999"
        val pseudoId = UUID.randomUUID()
        opprettTestdata(fnr, pseudoId)
        assertEquals(pseudoId, personDao.finnPseudoID(fnr.somNaturligIdent()))
    }

    @Test
    fun `kan finne naturlig ident fra pseudo id`() {
        val fnr = "12121255888"

        val pseudoId = UUID.randomUUID()
        opprettTestdata(fnr, pseudoId)
        assertEquals(fnr, personDao.finnNaturligIdent(pseudoId)!!.naturligIdent)
    }

    @Test
    fun `ukjent pseudoID returnerer null`() {
        assertNull(personDao.finnNaturligIdent(UUID.fromString("00000000-0000-0000-0000-000000000000")))
    }

    private fun opprettTestdata(
        fnr: String,
        pseudoId: UUID,
    ) {
        personDao.opprettPseudoId(pseudoId, NaturligIdent(fnr))
    }
}
