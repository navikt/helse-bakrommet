package no.nav.helse.bakrommet.db.dao

import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.domain.enNaturligIdent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.util.*
import kotlin.test.assertEquals

internal class PersonDaoTest {
    val db = MedDataSource(TestDataSource.dbModule.dataSource)
    private val personDao = PersonPseudoIdDaoPg(TestDataSource.dbModule.dataSource)

    @Test
    fun `returnerer spillerom-ID for kjent person-ID`() {
        val naturligIdent = enNaturligIdent()
        val pseudoId = UUID.randomUUID()
        personDao.opprettPseudoId(pseudoId, naturligIdent)
        assertEquals(pseudoId, personDao.finnPseudoID(naturligIdent))
    }

    @Test
    fun `kan finne naturlig ident fra pseudo id`() {
        val naturligIdent = enNaturligIdent()

        val pseudoId = UUID.randomUUID()
        personDao.opprettPseudoId(pseudoId, naturligIdent)
        assertEquals(naturligIdent, personDao.finnNaturligIdent(pseudoId))
    }

    @Test
    fun `ukjent pseudoID returnerer null`() {
        assertNull(personDao.finnNaturligIdent(UUID.fromString("00000000-0000-0000-0000-000000000000")))
    }
}
