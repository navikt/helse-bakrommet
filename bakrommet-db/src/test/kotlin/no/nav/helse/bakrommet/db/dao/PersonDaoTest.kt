package no.nav.helse.bakrommet.db.dao

import no.nav.helse.bakrommet.db.DBTestFixture
import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals

internal class PersonDaoTest {
    val db = MedDataSource(DBTestFixture.module.dataSource)
    private val personDao = PersonPseudoIdDaoPg(DBTestFixture.module.dataSource)

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

    @Test
    fun `slettPseudoIderEldreEnn sletter bare poster eldre enn angitt tidspunkt`() {
        val p1 = UUID.randomUUID()
        val fnr1 = enNaturligIdent()
        val p2 = UUID.randomUUID()
        val fnr2 = enNaturligIdent()
        val p3 = UUID.randomUUID()
        val fnr3 = enNaturligIdent()

        // Opprett poster med forskjellige tidspunkter
        personDao.opprettPseudoId(p1, fnr1)
        db.opprettPseudoIdMedDato(p2, fnr2, OffsetDateTime.now().minusDays(6))
        db.opprettPseudoIdMedDato(p3, fnr3, OffsetDateTime.now().minusDays(8))

        assertEquals(fnr1, personDao.finnNaturligIdent(p1))
        assertEquals(fnr2, personDao.finnNaturligIdent(p2))
        assertEquals(fnr3, personDao.finnNaturligIdent(p3))

        // Slett poster eldre enn 7 dager
        personDao.slettPseudoIderEldreEnn(OffsetDateTime.now().minusDays(7))

        assertEquals(fnr1, personDao.finnNaturligIdent(p1))
        assertEquals(fnr2, personDao.finnNaturligIdent(p2))
        assertNull(personDao.finnNaturligIdent(p3))

        // Slett poster eldre enn 5 dager
        personDao.slettPseudoIderEldreEnn(OffsetDateTime.now().minusDays(5))

        assertEquals(fnr1, personDao.finnNaturligIdent(p1))
        assertNull(personDao.finnNaturligIdent(p2))
        assertNull(personDao.finnNaturligIdent(p3))

        // Slett alle poster (eldre enn -1 dag = fremover i tid)
        personDao.slettPseudoIderEldreEnn(OffsetDateTime.now().plusDays(1))

        assertNull(personDao.finnNaturligIdent(p1))
        assertNull(personDao.finnNaturligIdent(p2))
        assertNull(personDao.finnNaturligIdent(p3))
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
