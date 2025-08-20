package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
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
            TestDataSource.resetDatasource()
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
        val saksbehandler = Bruker("Z12345", "Ola Nordmann", "ola@nav.no", emptySet())
        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)

        val periode =
            Saksbehandlingsperiode(
                id = id,
                spilleromPersonId = personId,
                opprettet = now,
                opprettetAvNavIdent = saksbehandler.navIdent,
                opprettetAvNavn = saksbehandler.navn,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = fom,
            ).truncateTidspunkt()
        dao.opprettPeriode(periode)

        val hentet = dao.finnSaksbehandlingsperiode(id)!!
        assertEquals(periode, hentet)

        val perioder = dao.finnPerioderForPerson(personId)
        assertEquals(1, perioder.size)
        assertEquals(periode, perioder[0])
    }

    @Test
    fun `kan finne perioder som overlapper angitt periode`() {
        val personId = "6512a"

        fun String.toLocalDate() = LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)

        fun opprettPeriode(
            fom: String,
            tom: String,
        ): Saksbehandlingsperiode {
            val id = UUID.randomUUID()
            val now = OffsetDateTime.now()
            val saksbehandler = Bruker("Z12345", "Ola Nordmann", "ola@nav.no", emptySet())
            val periode =
                Saksbehandlingsperiode(
                    id = id,
                    spilleromPersonId = personId,
                    opprettet = now,
                    opprettetAvNavIdent = saksbehandler.navIdent,
                    opprettetAvNavn = saksbehandler.navn,
                    fom = fom.toLocalDate(),
                    tom = tom.toLocalDate(),
                    skjæringstidspunkt = fom.toLocalDate(),
                ).truncateTidspunkt()
            dao.opprettPeriode(periode)
            return dao.finnSaksbehandlingsperiode(id)!!
        }

        fun finnOverlappende(
            fom: String,
            tom: String,
        ): Set<Saksbehandlingsperiode> {
            return dao.finnPerioderForPersonSomOverlapper(
                Companion.personId,
                fom.toLocalDate(),
                tom.toLocalDate(),
            ).toSet()
        }

        val p1 = opprettPeriode("2024-01-01", "2024-02-01")
        val p2 = opprettPeriode("2024-02-15", "2024-02-25")

        assertEquals(setOf(p1), finnOverlappende("2023-12-15", "2024-01-01"))
        assertEquals(emptySet(), finnOverlappende("2023-12-15", "2023-12-31"))
        assertEquals(setOf(p1, p2), finnOverlappende("2023-12-15", "2024-02-15"))
        assertEquals(setOf(p1), finnOverlappende("2023-12-15", "2024-02-14"))
        assertEquals(emptySet(), finnOverlappende("2024-02-02", "2024-02-14"))
        assertEquals(setOf(p1), finnOverlappende("2024-02-01", "2024-02-14"))
        assertEquals(setOf(p1, p2), finnOverlappende("2024-02-01", "2024-02-15"))
        assertEquals(setOf(p2), finnOverlappende("2024-02-25", "2024-03-15"))
        assertEquals(emptySet(), finnOverlappende("2024-02-26", "2024-03-15"))
    }
}
