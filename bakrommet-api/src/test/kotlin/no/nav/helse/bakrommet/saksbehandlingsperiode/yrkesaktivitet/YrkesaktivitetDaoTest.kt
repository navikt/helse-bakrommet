package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.testutils.tidsstuttet
import no.nav.helse.dto.PeriodeDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals

class YrkesaktivitetDaoTest {
    val dataSource = TestDataSource.dbModule.dataSource
    val fnr = "01019012345"
    val personId = "0h0a1"
    val saksbehandler = Bruker("ABC", "A. B. C", "ola@nav.no", emptySet())
    val periode =
        Saksbehandlingsperiode(
            id = UUID.randomUUID(),
            spilleromPersonId = personId,
            opprettet = OffsetDateTime.now(),
            opprettetAvNavIdent = saksbehandler.navIdent,
            opprettetAvNavn = saksbehandler.navn,
            fom = LocalDate.now().minusMonths(1),
            tom = LocalDate.now().minusDays(1),
            skjæringstidspunkt = LocalDate.now().minusMonths(1),
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
        val dao = YrkesaktivitetDao(dataSource)
        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = arbeidstakerKategorisering(),
                kategoriseringGenerert = null,
                dagoversikt = emptyList(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = periode.id,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )
        val ekko =
            dao.opprettYrkesaktivitet(
                id = yrkesaktivitetDbRecord.id,
                kategorisering = YrkesaktivitetKategoriseringMapper.fromMap(yrkesaktivitetDbRecord.kategorisering),
                dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
                saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
                opprettet = yrkesaktivitetDbRecord.opprettet,
                generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
                perioder = yrkesaktivitetDbRecord.perioder,
            )
        assertEquals(yrkesaktivitetDbRecord.tidsstuttet(), ekko.tidsstuttet())

        assertEquals(ekko, dao.hentYrkesaktivitetDbRecord(ekko.id))

        assertEquals(listOf(ekko), dao.hentYrkesaktivitetFor(periode))
    }

    @Test
    fun `inntektsforhold må referere gyldig saksbehandlingsperiode`() {
        val dao = YrkesaktivitetDao(dataSource)
        val ugyldigPeriodeId = UUID.randomUUID()
        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = arbeidstakerKategorisering(),
                kategoriseringGenerert = null,
                dagoversikt = emptyList(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = ugyldigPeriodeId,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        assertThrows<SQLException> {
            dao.opprettYrkesaktivitet(
                id = yrkesaktivitetDbRecord.id,
                kategorisering = YrkesaktivitetKategoriseringMapper.fromMap(yrkesaktivitetDbRecord.kategorisering),
                dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
                saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
                opprettet = yrkesaktivitetDbRecord.opprettet,
                generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
                perioder = yrkesaktivitetDbRecord.perioder,
            )
        }
    }

    @Test
    fun `oppdaterer perioder for inntektsforhold`() {
        val dao = YrkesaktivitetDao(dataSource)
        val yrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = arbeidstakerKategorisering(),
                kategoriseringGenerert = null,
                dagoversikt = emptyList(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = periode.id,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
                perioder = null,
            )
        val opprettetYrkesaktivitet =
            dao.opprettYrkesaktivitet(
                id = yrkesaktivitetDbRecord.id,
                kategorisering = YrkesaktivitetKategoriseringMapper.fromMap(yrkesaktivitetDbRecord.kategorisering),
                dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
                saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
                opprettet = yrkesaktivitetDbRecord.opprettet,
                generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
                perioder = yrkesaktivitetDbRecord.perioder,
            )

        // Oppdater perioder
        val perioder =
            Perioder(
                type = Periodetype.ARBEIDSGIVERPERIODE,
                perioder =
                    listOf(
                        PeriodeDto(
                            fom = LocalDate.of(2023, 1, 1),
                            tom = LocalDate.of(2023, 1, 15),
                        ),
                    ),
            )

        val oppdatertYrkesaktivitet = dao.oppdaterPerioder(opprettetYrkesaktivitet, perioder)
        assertEquals(perioder, oppdatertYrkesaktivitet.perioder)

        // Slett perioder
        val yrkesaktivitetUtenPerioder = dao.oppdaterPerioder(opprettetYrkesaktivitet, null)
        assertEquals(null, yrkesaktivitetUtenPerioder.perioder)
    }
}
