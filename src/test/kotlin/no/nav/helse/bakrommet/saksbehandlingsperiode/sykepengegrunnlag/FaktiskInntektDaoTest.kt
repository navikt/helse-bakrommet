package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDao
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FaktiskInntektDaoTest {
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
        )
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

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        val personDao = PersonDao(dataSource)
        personDao.opprettPerson(fnr, personId)
        val behandlingDao = SaksbehandlingsperiodeDao(dataSource)
        behandlingDao.opprettPeriode(periode)
        val inntektsforholdDao = InntektsforholdDao(dataSource)
        inntektsforholdDao.opprettInntektsforhold(inntektsforhold)
    }

    @Test
    fun `oppretter og henter faktisk inntekt`() {
        val dao = FaktiskInntektDao(dataSource)
        val faktiskInntekt =
            FaktiskInntekt(
                id = null,
                inntektsforholdId = inntektsforhold.id,
                beløpPerMånedØre = 4500000L,
                kilde = Inntektskilde.SAKSBEHANDLER,
                erSkjønnsfastsatt = false,
                skjønnsfastsettelseBegrunnelse = null,
                refusjon = null,
                opprettetAv = saksbehandler.navIdent,
            )

        val lagretInntekt = dao.opprettFaktiskInntekt(faktiskInntekt)

        assertNotNull(lagretInntekt.id)
        assertEquals(faktiskInntekt.inntektsforholdId, lagretInntekt.inntektsforholdId)
        assertEquals(faktiskInntekt.beløpPerMånedØre, lagretInntekt.beløpPerMånedØre)
        assertEquals(faktiskInntekt.kilde, lagretInntekt.kilde)
        assertEquals(faktiskInntekt.erSkjønnsfastsatt, lagretInntekt.erSkjønnsfastsatt)
        assertEquals(faktiskInntekt.opprettetAv, lagretInntekt.opprettetAv)

        val hentetInntekt = dao.hentFaktiskInntekt(lagretInntekt.id!!)
        assertEquals(lagretInntekt, hentetInntekt)
    }

    @Test
    fun `oppretter faktisk inntekt med refusjon`() {
        val dao = FaktiskInntektDao(dataSource)
        val refusjon =
            Refusjonsforhold(
                refusjonsbeløpPerMånedØre = 4500000L,
                refusjonsgrad = 100,
            )
        val faktiskInntekt =
            FaktiskInntekt(
                id = null,
                inntektsforholdId = inntektsforhold.id,
                beløpPerMånedØre = 4500000L,
                kilde = Inntektskilde.SAKSBEHANDLER,
                erSkjønnsfastsatt = false,
                skjønnsfastsettelseBegrunnelse = null,
                refusjon = refusjon,
                opprettetAv = saksbehandler.navIdent,
            )

        val lagretInntekt = dao.opprettFaktiskInntekt(faktiskInntekt)

        assertNotNull(lagretInntekt.refusjon)
        assertEquals(refusjon.refusjonsbeløpPerMånedØre, lagretInntekt.refusjon!!.refusjonsbeløpPerMånedØre)
        assertEquals(refusjon.refusjonsgrad, lagretInntekt.refusjon!!.refusjonsgrad)
    }

    @Test
    fun `oppretter faktisk inntekt med skjønnsfastsettelse`() {
        val dao = FaktiskInntektDao(dataSource)
        val faktiskInntekt =
            FaktiskInntekt(
                id = null,
                inntektsforholdId = inntektsforhold.id,
                beløpPerMånedØre = 5000000L,
                kilde = Inntektskilde.SKJONNSFASTSETTELSE,
                erSkjønnsfastsatt = true,
                skjønnsfastsettelseBegrunnelse = "Fastsatt skjønnsmessig grunnet manglende dokumentasjon",
                refusjon = null,
                opprettetAv = saksbehandler.navIdent,
            )

        val lagretInntekt = dao.opprettFaktiskInntekt(faktiskInntekt)

        assertEquals(true, lagretInntekt.erSkjønnsfastsatt)
        assertEquals(
            "Fastsatt skjønnsmessig grunnet manglende dokumentasjon",
            lagretInntekt.skjønnsfastsettelseBegrunnelse,
        )
        assertEquals(Inntektskilde.SKJONNSFASTSETTELSE, lagretInntekt.kilde)
    }

    @Test
    fun `henter faktiske inntekter for saksbehandlingsperiode`() {
        val dao = FaktiskInntektDao(dataSource)

        // Opprett flere faktiske inntekter
        val inntekt1 =
            FaktiskInntekt(
                id = null,
                inntektsforholdId = inntektsforhold.id,
                beløpPerMånedØre = 4000000L,
                kilde = Inntektskilde.SAKSBEHANDLER,
                erSkjønnsfastsatt = false,
                skjønnsfastsettelseBegrunnelse = null,
                refusjon = null,
                opprettetAv = saksbehandler.navIdent,
            )

        val inntekt2 =
            FaktiskInntekt(
                id = null,
                inntektsforholdId = inntektsforhold.id,
                beløpPerMånedØre = 1500000L,
                kilde = Inntektskilde.AINNTEKT,
                erSkjønnsfastsatt = false,
                skjønnsfastsettelseBegrunnelse = null,
                refusjon = null,
                opprettetAv = saksbehandler.navIdent,
            )

        dao.opprettFaktiskInntekt(inntekt1)
        dao.opprettFaktiskInntekt(inntekt2)

        val faktiskeInntekter = dao.hentFaktiskeInntekterFor(periode.id)

        assertEquals(2, faktiskeInntekter.size)
        // Nyeste først (ORDER BY opprettet DESC)
        assertEquals(1500000L, faktiskeInntekter[0].beløpPerMånedØre)
        assertEquals(4000000L, faktiskeInntekter[1].beløpPerMånedØre)
    }

    @Test
    fun `sletter faktiske inntekter for saksbehandlingsperiode`() {
        val dao = FaktiskInntektDao(dataSource)

        val faktiskInntekt =
            FaktiskInntekt(
                id = null,
                inntektsforholdId = inntektsforhold.id,
                beløpPerMånedØre = 4500000L,
                kilde = Inntektskilde.SAKSBEHANDLER,
                erSkjønnsfastsatt = false,
                skjønnsfastsettelseBegrunnelse = null,
                refusjon = null,
                opprettetAv = saksbehandler.navIdent,
            )

        val lagretInntekt = dao.opprettFaktiskInntekt(faktiskInntekt)

        // Verifiser at inntekten finnes
        assertEquals(1, dao.hentFaktiskeInntekterFor(periode.id).size)

        // Slett alle faktiske inntekter for perioden
        dao.slettFaktiskeInntekterFor(periode.id)

        // Verifiser at inntektene er slettet
        assertEquals(0, dao.hentFaktiskeInntekterFor(periode.id).size)
        assertNull(dao.hentFaktiskInntekt(lagretInntekt.id!!))
    }

    @Test
    fun `faktisk inntekt må referere gyldig inntektsforhold`() {
        val dao = FaktiskInntektDao(dataSource)
        val ugyldigInntektsforholdId = UUID.randomUUID()

        val faktiskInntekt =
            FaktiskInntekt(
                id = null,
                inntektsforholdId = ugyldigInntektsforholdId,
                beløpPerMånedØre = 4500000L,
                kilde = Inntektskilde.SAKSBEHANDLER,
                erSkjønnsfastsatt = false,
                skjønnsfastsettelseBegrunnelse = null,
                refusjon = null,
                opprettetAv = saksbehandler.navIdent,
            )

        assertThrows<SQLException> {
            dao.opprettFaktiskInntekt(faktiskInntekt)
        }
    }

    @Test
    fun `returnerer tom liste for periode uten faktiske inntekter`() {
        val dao = FaktiskInntektDao(dataSource)
        val annenPeriode = UUID.randomUUID()

        val faktiskeInntekter = dao.hentFaktiskeInntekterFor(annenPeriode)

        assertEquals(0, faktiskeInntekter.size)
    }
}
