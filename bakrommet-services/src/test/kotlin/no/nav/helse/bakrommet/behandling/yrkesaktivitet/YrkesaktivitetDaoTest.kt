package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.BehandlingDaoPg
import no.nav.helse.bakrommet.behandling.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.person.PersonPseudoIdDaoPg
import no.nav.helse.bakrommet.testutils.tidsstuttet
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        Behandling(
            id = UUID.randomUUID(),
            naturligIdent = personId,
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
        val dao = PersonPseudoIdDaoPg(dataSource)
        dao.opprettPerson(fnr, personId)
        val behandlingDao = BehandlingDaoPg(dataSource)
        behandlingDao.opprettPeriode(periode)
    }

    @Test
    fun `oppretter og henter yrkesaktivitet`() {
        val dao = YrkesaktivitetDaoPg(dataSource)
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
                inntektRequest = null,
                inntektData = null,
                refusjon = null,
            )
        val ekko =
            dao.opprettYrkesaktivitet(
                id = yrkesaktivitetDbRecord.id,
                kategorisering = yrkesaktivitetDbRecord.kategorisering,
                dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
                saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
                opprettet = yrkesaktivitetDbRecord.opprettet,
                generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
                perioder = yrkesaktivitetDbRecord.perioder,
                inntektData = yrkesaktivitetDbRecord.inntektData,
                refusjonsdata = yrkesaktivitetDbRecord.refusjon,
            )
        assertEquals(yrkesaktivitetDbRecord.tidsstuttet(), ekko.tidsstuttet())

        assertEquals(ekko, dao.hentYrkesaktivitetDbRecord(ekko.id))

        assertEquals(listOf(ekko), dao.hentYrkesaktiviteterDbRecord(periode))
    }

    @Test
    fun `yrkesaktivitet må referere gyldig saksbehandlingsperiode`() {
        val dao = YrkesaktivitetDaoPg(dataSource)
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
                inntektRequest = null,
                inntektData = null,
                refusjon = null,
            )

        assertThrows<KunneIkkeOppdatereDbException> {
            dao.opprettYrkesaktivitet(
                id = yrkesaktivitetDbRecord.id,
                kategorisering = yrkesaktivitetDbRecord.kategorisering,
                dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
                saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
                opprettet = yrkesaktivitetDbRecord.opprettet,
                generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
                perioder = yrkesaktivitetDbRecord.perioder,
                inntektData = yrkesaktivitetDbRecord.inntektData,
                refusjonsdata = yrkesaktivitetDbRecord.refusjon,
            )
        }
    }

    @Test
    fun `oppdaterer perioder for yrkesaktivitet`() {
        val dao = YrkesaktivitetDaoPg(dataSource)
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
                inntektRequest = null,
                inntektData = null,
                refusjon = null,
            )
        val opprettetYrkesaktivitet =
            dao.opprettYrkesaktivitet(
                id = yrkesaktivitetDbRecord.id,
                kategorisering = yrkesaktivitetDbRecord.kategorisering,
                dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
                saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
                opprettet = yrkesaktivitetDbRecord.opprettet,
                generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
                perioder = yrkesaktivitetDbRecord.perioder,
                inntektData = yrkesaktivitetDbRecord.inntektData,
                refusjonsdata = yrkesaktivitetDbRecord.refusjon,
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

    @Test
    fun `oppdaterer og henter inntektRequest for yrkesaktivitet`() {
        val dao = YrkesaktivitetDaoPg(dataSource)
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
                inntektRequest = null,
                inntektData = null,
                refusjon = null,
            )

        // Opprett yrkesaktivitet
        val opprettetYrkesaktivitet =
            dao.opprettYrkesaktivitet(
                id = yrkesaktivitetDbRecord.id,
                kategorisering = yrkesaktivitetDbRecord.kategorisering,
                dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
                saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
                opprettet = yrkesaktivitetDbRecord.opprettet,
                generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
                perioder = yrkesaktivitetDbRecord.perioder,
                inntektData = yrkesaktivitetDbRecord.inntektData,
                refusjonsdata = yrkesaktivitetDbRecord.refusjon,
            )

        // Verifiser at inntektRequest er null ved opprettelse
        assertEquals(null, opprettetYrkesaktivitet.inntektRequest)

        // Opprett en InntektRequest
        val inntektRequest =
            InntektRequest.Arbeidstaker(
                data =
                    ArbeidstakerInntektRequest.Inntektsmelding(
                        inntektsmeldingId = "123456",
                        begrunnelse = "Test begrunnelse",
                    ),
            )

        // Oppdater inntektRequest
        val oppdatertYrkesaktivitet =
            dao.oppdaterInntektrequest(
                Yrkesaktivitet(
                    id = opprettetYrkesaktivitet.id,
                    kategorisering = opprettetYrkesaktivitet.kategorisering,
                    kategoriseringGenerert = opprettetYrkesaktivitet.kategoriseringGenerert,
                    dagoversikt = opprettetYrkesaktivitet.dagoversikt,
                    dagoversiktGenerert = opprettetYrkesaktivitet.dagoversiktGenerert,
                    saksbehandlingsperiodeId = opprettetYrkesaktivitet.saksbehandlingsperiodeId,
                    opprettet = opprettetYrkesaktivitet.opprettet,
                    generertFraDokumenter = opprettetYrkesaktivitet.generertFraDokumenter,
                    perioder = opprettetYrkesaktivitet.perioder,
                    inntektRequest = null,
                    inntektData = null,
                ),
                inntektRequest,
            )

        // Verifiser at inntektRequest er oppdatert
        assertEquals(inntektRequest, oppdatertYrkesaktivitet.inntektRequest)

        // Hent yrkesaktivitet og verifiser at inntektRequest er lagret
        val hentetYrkesaktivitet = dao.hentYrkesaktivitet(opprettetYrkesaktivitet.id)
        assertEquals(inntektRequest, hentetYrkesaktivitet?.inntektRequest)

        // Hent via hentYrkesaktivitetDbRecord og verifiser
        val hentetDbRecord = dao.hentYrkesaktivitetDbRecord(opprettetYrkesaktivitet.id)
        assertEquals(inntektRequest, hentetDbRecord?.inntektRequest)
    }

    @Test
    fun `oppdaterer og henter inntektData for yrkesaktivitet`() {
        val dao = YrkesaktivitetDaoPg(dataSource)
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
                inntektRequest = null,
                inntektData = null,
                refusjon = null,
            )

        // Opprett yrkesaktivitet
        val opprettetYrkesaktivitet =
            dao.opprettYrkesaktivitet(
                id = yrkesaktivitetDbRecord.id,
                kategorisering = yrkesaktivitetDbRecord.kategorisering,
                dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
                saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
                opprettet = yrkesaktivitetDbRecord.opprettet,
                generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
                perioder = yrkesaktivitetDbRecord.perioder,
                inntektData = yrkesaktivitetDbRecord.inntektData,
                refusjonsdata = yrkesaktivitetDbRecord.refusjon,
            )

        // Verifiser at inntektData er null ved opprettelse
        assertEquals(null, opprettetYrkesaktivitet.inntektData)

        // Opprett en InntektData
        val inntektData =
            InntektData.ArbeidstakerInntektsmelding(
                inntektsmeldingId = "123456",
                omregnetÅrsinntekt = InntektbeløpDto.Årlig(500000.0),
                inntektsmelding = JsonNodeFactory.instance.objectNode(),
            )

        // Oppdater inntektData
        val oppdatertYrkesaktivitet =
            dao.oppdaterInntektData(
                Yrkesaktivitet(
                    id = opprettetYrkesaktivitet.id,
                    kategorisering = opprettetYrkesaktivitet.kategorisering,
                    kategoriseringGenerert = opprettetYrkesaktivitet.kategoriseringGenerert,
                    dagoversikt = opprettetYrkesaktivitet.dagoversikt,
                    dagoversiktGenerert = opprettetYrkesaktivitet.dagoversiktGenerert,
                    saksbehandlingsperiodeId = opprettetYrkesaktivitet.saksbehandlingsperiodeId,
                    opprettet = opprettetYrkesaktivitet.opprettet,
                    generertFraDokumenter = opprettetYrkesaktivitet.generertFraDokumenter,
                    perioder = opprettetYrkesaktivitet.perioder,
                    inntektRequest = null,
                    inntektData = null,
                ),
                inntektData,
            )

        // Verifiser at inntektData er oppdatert
        assertEquals(inntektData, oppdatertYrkesaktivitet.inntektData)

        // Hent yrkesaktivitet og verifiser at inntektData er lagret
        val hentetYrkesaktivitet = dao.hentYrkesaktivitet(opprettetYrkesaktivitet.id)
        assertEquals(inntektData, hentetYrkesaktivitet?.inntektData)

        // Hent via hentYrkesaktivitetDbRecord og verifiser
        val hentetDbRecord = dao.hentYrkesaktivitetDbRecord(opprettetYrkesaktivitet.id)
        assertEquals(inntektData, hentetDbRecord?.inntektData)
    }

    @Test
    fun `oppdaterer og henter refusjonsdata for yrkesaktivitet`() {
        val dao = YrkesaktivitetDaoPg(dataSource)
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
                inntektRequest = null,
                inntektData = null,
                refusjon = null,
            )

        // Opprett yrkesaktivitet
        val opprettetYrkesaktivitet =
            dao.opprettYrkesaktivitet(
                id = yrkesaktivitetDbRecord.id,
                kategorisering = yrkesaktivitetDbRecord.kategorisering,
                dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
                saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
                opprettet = yrkesaktivitetDbRecord.opprettet,
                generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
                perioder = yrkesaktivitetDbRecord.perioder,
                inntektData = yrkesaktivitetDbRecord.inntektData,
                refusjonsdata = yrkesaktivitetDbRecord.refusjon,
            )

        // Verifiser at refusjonsdata er null ved opprettelse
        assertEquals(null, opprettetYrkesaktivitet.refusjon)

        // Opprett refusjonsdata
        val refusjonsdata =
            listOf(
                Refusjonsperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 15),
                    beløp = InntektbeløpDto.MånedligDouble(50000.0),
                ),
                Refusjonsperiode(
                    fom = LocalDate.of(2023, 2, 1),
                    tom = null, // Åpen periode
                    beløp = InntektbeløpDto.MånedligDouble(60000.0),
                ),
            )

        // Oppdater refusjonsdata
        val oppdatertYrkesaktivitet =
            dao.oppdaterRefusjon(
                opprettetYrkesaktivitet.id,
                refusjonsdata,
            )

        // Verifiser at refusjonsdata er oppdatert
        assertEquals(refusjonsdata, oppdatertYrkesaktivitet.refusjon)

        // Hent yrkesaktivitet og verifiser at refusjonsdata er lagret
        val hentetYrkesaktivitet = dao.hentYrkesaktivitet(opprettetYrkesaktivitet.id)
        assertEquals(refusjonsdata, hentetYrkesaktivitet?.refusjon)

        // Hent via hentYrkesaktivitetDbRecord og verifiser
        val hentetDbRecord = dao.hentYrkesaktivitetDbRecord(opprettetYrkesaktivitet.id)
        assertEquals(refusjonsdata, hentetDbRecord?.refusjon)

        // Test at vi kan sette refusjonsdata til null
        val yrkesaktivitetUtenRefusjonsdata =
            dao.oppdaterRefusjon(
                opprettetYrkesaktivitet.id,
                null,
            )

        assertEquals(null, yrkesaktivitetUtenRefusjonsdata.refusjon)
    }
}
