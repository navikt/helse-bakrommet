package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals

internal class SaksbehandlingsperiodeDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource

    private val dao = SaksbehandlingsperiodeDao(dataSource)

    companion object {
        val fnr = "01019012345"
        val personId = "6512a"

        @JvmStatic
        @BeforeAll
        fun setOpp() {
            val dao = PersonDao(TestDataSource.dbModule.dataSource)
            dao.opprettPerson(fnr, personId)
        }
    }

    @Test
    fun `ukjent id gir null`() {
        assertNull(dao.finnSaksbehandlingsperiode(UUID.randomUUID()))
    }

    @Test
    fun `kan opprette og hente periode`() {
        val id = UUID.randomUUID()
        val personId = "6512a"
        val now = OffsetDateTime.now()
        val navIdent = "Z12345"
        val navNavn = "Ola Nordmann"
        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)

        val periode =
            Saksbehandlingsperiode(
                id = id,
                spilleromPersonId = personId,
                opprettet = now,
                opprettetAvNavIdent = navIdent,
                opprettetAvNavn = navNavn,
                fom = fom,
                tom = tom,
            ).truncateTidspunkt()
        dao.opprettPeriode(periode)

        val hentet = dao.finnSaksbehandlingsperiode(id)!!
        assertEquals(periode, hentet)

        val perioder = dao.finnPerioderForPerson(personId)
        assertEquals(1, perioder.size)
        assertEquals(periode, perioder[0])
    }
}
