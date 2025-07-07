package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.nav.Saksbehandler
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.testutils.tidsstuttet
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals

class InntektsforholdDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource
    val fnr = "01019012345"
    val personId = "0h0a1"
    val saksbehandler = Saksbehandler("ABC", "A. B. C", "ola@nav.no")
    val periode =
        Saksbehandlingsperiode(
            id = UUID.randomUUID(),
            spilleromPersonId = personId,
            opprettet = OffsetDateTime.now(),
            opprettetAvNavIdent = saksbehandler.navIdent,
            opprettetAvNavn = saksbehandler.navn,
            fom = LocalDate.now().minusMonths(1),
            tom = LocalDate.now().minusDays(1),
        )

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        val dao = PersonDao(dataSource)
        dao.opprettPerson(fnr, personId)
        val behandlingDao = SaksbehandlingsperiodeDao(dataSource)
        behandlingDao.opprettPeriode(periode)
    }

    @Test
    fun `oppretter og henter inntektsforhold`() {
        val dao = InntektsforholdDao(dataSource)
        val inntektsforhold =
            Inntektsforhold(
                id = UUID.randomUUID(),
                kategorisering = """{"INNTEKTSKATEGORI": "ARBEIDSTAKER"}""".asJsonNode(),
                kategoriseringGenerert = null,
                dagoversikt = """[]""".asJsonNode(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = periode.id,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )
        val ekko = dao.opprettInntektsforhold(inntektsforhold)
        assertEquals(inntektsforhold.tidsstuttet(), ekko.tidsstuttet())

        assertEquals(ekko, dao.hentInntektsforhold(ekko.id))

        assertEquals(listOf(ekko), dao.hentInntektsforholdFor(periode))
    }

    @Test
    fun `inntektsforhold må referere gyldig saksbehandlingsperiode`() {
        val dao = InntektsforholdDao(dataSource)
        val ugyldigPeriodeId = UUID.randomUUID()
        val inntektsforhold =
            Inntektsforhold(
                id = UUID.randomUUID(),
                kategorisering = """{"INNTEKTSKATEGORI": "ARBEIDSTAKER"}""".asJsonNode(),
                kategoriseringGenerert = null,
                dagoversikt = """[]""".asJsonNode(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = ugyldigPeriodeId,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        assertThrows<SQLException> {
            dao.opprettInntektsforhold(inntektsforhold)
        }
    }
}
