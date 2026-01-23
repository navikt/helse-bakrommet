package no.nav.helse.bakrommet.person

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.db.DBTestFixture
import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.skapDbDaoer
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.pdl.PdlMock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals

class PersonServiceTest {
    private val dataSource = DBTestFixture.module.dataSource
    private val qr: QueryRunner = MedDataSource(dataSource)
    private val db: DbDaoer<AlleDaoer> = skapDbDaoer(dataSource)
    private val personService: PersonService = PersonService(db, personinfoProvider = PdlMock.pdlClient())

    @Test
    fun `slettPseudoIderEldreEnn N antall dager, gjør nettopp det`() {
        val p1 = UUID.randomUUID()
        val fnr1 = enNaturligIdent()
        val p2 = UUID.randomUUID()
        val fnr2 = enNaturligIdent()
        val p3 = UUID.randomUUID()
        val fnr3 = enNaturligIdent()

        db.opprettPseudoIdMedDAO(p1, fnr1)
        qr.opprettPseudoIdMedDato(p2, fnr2, OffsetDateTime.now().minusDays(6))
        qr.opprettPseudoIdMedDato(p3, fnr3, OffsetDateTime.now().minusDays(8))

        assertEquals(fnr1, runBlocking { personService.finnNaturligIdent(p1) })
        assertEquals(fnr2, runBlocking { personService.finnNaturligIdent(p2) })
        assertEquals(fnr3, runBlocking { personService.finnNaturligIdent(p3) })

        runBlocking { personService.slettPseudoIderEldreEnn(7) }

        assertEquals(fnr1, runBlocking { personService.finnNaturligIdent(p1) })
        assertEquals(fnr2, runBlocking { personService.finnNaturligIdent(p2) })
        assertNull(runBlocking { personService.finnNaturligIdent(p3) })

        runBlocking { personService.slettPseudoIderEldreEnn(5) }

        assertEquals(fnr1, runBlocking { personService.finnNaturligIdent(p1) })
        assertNull(runBlocking { personService.finnNaturligIdent(p2) })
        assertNull(runBlocking { personService.finnNaturligIdent(p3) })

        runBlocking { personService.slettPseudoIderEldreEnn(-1) }

        assertNull(runBlocking { personService.finnNaturligIdent(p1) })
        assertNull(runBlocking { personService.finnNaturligIdent(p2) })
        assertNull(runBlocking { personService.finnNaturligIdent(p3) })
    }
}

private fun DbDaoer<AlleDaoer>.opprettPseudoIdMedDAO(
    pseudoId: UUID,
    naturligIdent: NaturligIdent,
) = runBlocking {
    nonTransactional {
        personPseudoIdDao.opprettPseudoId(pseudoId, naturligIdent)
    }
}

private fun QueryRunner.opprettPseudoIdMedDato(
    pseudoId: UUID,
    naturligIdent: NaturligIdent,
    opprettet: OffsetDateTime,
) {
    update(
        """
        INSERT INTO person_pseudo_id (pseudo_id, naturlig_ident, opprettet)
        VALUES (:pseudo_id, :naturlig_ident, :opprettet)
        """.trimIndent(),
        "pseudo_id" to pseudoId,
        "naturlig_ident" to naturligIdent.value,
        "opprettet" to opprettet,
    )
}
