package no.nav.helse.bakrommet.person

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.skapDbDaoer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals

class PersonServiceTest {
    private val dataSource = TestDataSource.dbModule.dataSource
    private val qr: QueryRunner = MedDataSource(dataSource)
    private lateinit var personService: PersonService
    private lateinit var db: DbDaoer<AlleDaoer>

    @BeforeEach
    fun setup() {
        TestDataSource.resetDatasource()
        db = skapDbDaoer(dataSource)
        personService = PersonService(db, pdlClient = PdlMock.pdlClient())
    }

    @Test
    fun `slettPseudoIderEldreEnn N antall dager, gjør nettopp det`() {
        val p1 = UUID.randomUUID()
        val fnr1 = NaturligIdent("01010111111")
        val p2 = UUID.randomUUID()
        val fnr2 = NaturligIdent("02020222222")
        val p3 = UUID.randomUUID()
        val fnr3 = NaturligIdent("03030333333")

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
        "naturlig_ident" to naturligIdent.naturligIdent,
        "opprettet" to opprettet,
    )
}
