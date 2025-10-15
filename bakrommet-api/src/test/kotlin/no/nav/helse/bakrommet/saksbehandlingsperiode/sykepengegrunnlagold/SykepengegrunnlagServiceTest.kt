package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlagold

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategoriseringMapper
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.arbeidstakerKategorisering
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.frilanserKategorisering
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SykepengegrunnlagServiceTest {
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

    lateinit var service: SykepengegrunnlagService

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        val personDao = PersonDao(dataSource)
        personDao.opprettPerson(fnr, personId)
        val behandlingDao = SaksbehandlingsperiodeDao(dataSource)
        behandlingDao.opprettPeriode(periode)
        val yrkesaktivitetDao = YrkesaktivitetDao(dataSource)
        yrkesaktivitetDao.opprettYrkesaktivitet(
            id = yrkesaktivitetDbRecord.id,
            kategorisering = YrkesaktivitetKategoriseringMapper.fromMap(yrkesaktivitetDbRecord.kategorisering),
            dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
            saksbehandlingsperiodeId = yrkesaktivitetDbRecord.saksbehandlingsperiodeId,
            opprettet = yrkesaktivitetDbRecord.opprettet,
            generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
            perioder = yrkesaktivitetDbRecord.perioder,
            inntektData = null,
        )

        val sykepengegrunnlagDaoOld = SykepengegrunnlagDaoOld(dataSource)
        val beregningDao = UtbetalingsberegningDao(dataSource)

        service =
            SykepengegrunnlagService(
                object : SykepengegrunnlagServiceDaoer {
                    override val saksbehandlingsperiodeDao = behandlingDao
                    override val yrkesaktivitetDao = yrkesaktivitetDao
                    override val sykepengegrunnlagDaoOld = sykepengegrunnlagDaoOld
                    override val beregningDao = beregningDao
                    override val personDao = personDao
                },
                TransactionalSessionFactory(dataSource) { session ->
                    object : SykepengegrunnlagServiceDaoer {
                        override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDao(session)
                        override val yrkesaktivitetDao = YrkesaktivitetDao(session)
                        override val sykepengegrunnlagDaoOld = SykepengegrunnlagDaoOld(session)
                        override val beregningDao = UtbetalingsberegningDao(session)
                        override val personDao = PersonDao(session)
                    }
                },
            )
    }

    private fun periodeReferanse() =
        SaksbehandlingsperiodeReferanse(
            spilleromPersonId = SpilleromPersonId(personId),
            periodeUUID = periode.id,
        )

    @Test
    fun `beregner sykepengegrunnlag uten 6G-begrensning`() {
        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            // 45 000 kr/måned
                            beløpPerMånedØre = 4500000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
                begrunnelse = "Standard beregning",
            )

        val resultat = service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // 45 000 * 12 = 540 000 kr
        assertEquals(54000000L, resultat.totalInntektØre)
        // 1G - sjekk at det er et gyldig grunnbeløp (enten 2024 eller 2025)
        assertTrue(resultat.grunnbeløpØre == 12402800L || resultat.grunnbeløpØre == 13016000L || resultat.grunnbeløpØre == 11147700L)
        // 6G - sjekk at det er et gyldig grunnbeløp (enten 2024 eller 2025)
        assertTrue(resultat.grunnbeløp6GØre == 74416800L || resultat.grunnbeløp6GØre == 78096000L || resultat.grunnbeløp6GØre == 66886200L)
        assertEquals(false, resultat.begrensetTil6G)
        // Ikke begrenset
        assertEquals(54000000L, resultat.sykepengegrunnlagØre)
        assertEquals("Standard beregning", resultat.begrunnelse)
        assertEquals(saksbehandler.navIdent, resultat.opprettetAv)
        assertEquals(1, resultat.inntekter.size)
    }

    @Test
    fun `beregner sykepengegrunnlag med 6G-begrensning`() {
        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            // 80 000 kr/måned
                            beløpPerMånedØre = 8000000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
            )

        val resultat = service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // 80 000 * 12 = 960 000 kr
        assertEquals(96000000L, resultat.totalInntektØre)
        // 1G - sjekk at det er et gyldig grunnbeløp (enten 2024 eller 2025)
        assertTrue(resultat.grunnbeløpØre == 12402800L || resultat.grunnbeløpØre == 13016000L || resultat.grunnbeløpØre == 11147700L)
        // 6G - sjekk at det er et gyldig grunnbeløp (enten 2024 eller 2025)
        assertTrue(resultat.grunnbeløp6GØre == 74416800L || resultat.grunnbeløp6GØre == 78096000L || resultat.grunnbeløp6GØre == 66886200L)
        assertEquals(true, resultat.begrensetTil6G)
        // Begrenset til 6G
        assertEquals(resultat.grunnbeløp6GØre, resultat.sykepengegrunnlagØre)
    }

    @Test
    fun `beregner sykepengegrunnlag med flere inntektsforhold`() {
        // Opprett et andre inntektsforhold
        val yrkesaktivitetDbRecord2 =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = frilanserKategorisering(),
                kategoriseringGenerert = null,
                dagoversikt = emptyList(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = periode.id,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )
        val yrkesaktivitetDao = YrkesaktivitetDao(dataSource)
        yrkesaktivitetDao.opprettYrkesaktivitet(
            id = yrkesaktivitetDbRecord2.id,
            kategorisering = YrkesaktivitetKategoriseringMapper.fromMap(yrkesaktivitetDbRecord2.kategorisering),
            dagoversikt = yrkesaktivitetDbRecord2.dagoversikt,
            saksbehandlingsperiodeId = yrkesaktivitetDbRecord2.saksbehandlingsperiodeId,
            opprettet = yrkesaktivitetDbRecord2.opprettet,
            generertFraDokumenter = yrkesaktivitetDbRecord2.generertFraDokumenter,
            perioder = yrkesaktivitetDbRecord2.perioder,
        )

        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            // 30 000 kr/måned
                            beløpPerMånedØre = 3000000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord2.id,
                            // 25 000 kr/måned
                            beløpPerMånedØre = 2500000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
                begrunnelse = "Kombinert beregning",
            )

        val resultat = service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // (30 000 + 25 000) * 12 = 660 000 kr
        assertEquals(66000000L, resultat.totalInntektØre)
        assertEquals(false, resultat.begrensetTil6G)
        assertEquals(66000000L, resultat.sykepengegrunnlagØre)
        assertEquals(2, resultat.inntekter.size)
    }

    @Test
    fun `oppretter sykepengegrunnlag med skjønnsfastsettelse og refusjon`() {
        val refusjon =
            listOf(
                Refusjonsperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 31),
                    beløpØre = 5000000L,
                ),
            )

        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            // 50 000 kr/måned
                            beløpPerMånedØre = 5000000L,
                            kilde = Inntektskilde.SKJONNSFASTSETTELSE,
                            refusjon = refusjon,
                        ),
                    ),
                begrunnelse = "Skjønnsfastsettelse med refusjon",
            )

        val resultat = service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // 50 000 * 12 = 600 000 kr
        assertEquals(60000000L, resultat.totalInntektØre)
        assertEquals(1, resultat.inntekter.size)

        val inntekt = resultat.inntekter[0]
        assertEquals(Inntektskilde.SKJONNSFASTSETTELSE, inntekt.kilde)
        assertEquals(Inntektskilde.SKJONNSFASTSETTELSE, inntekt.kilde)
        assertEquals(1, inntekt.refusjon.size)
        val refusjonsperiode = inntekt.refusjon[0]
        assertEquals(LocalDate.of(2023, 1, 1), refusjonsperiode.fom)
        assertEquals(LocalDate.of(2023, 1, 31), refusjonsperiode.tom)
        assertEquals(5000000L, refusjonsperiode.beløpØre)
    }

    @Test
    fun `oppretter sykepengegrunnlag med åpen refusjonsperiode`() {
        val refusjon =
            listOf(
                Refusjonsperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = null,
                    beløpØre = 5000000L,
                ),
            )

        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            // 50 000 kr/måned
                            beløpPerMånedØre = 5000000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = refusjon,
                        ),
                    ),
            )

        val resultat = service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        assertEquals(60000000L, resultat.totalInntektØre)
        assertEquals(1, resultat.inntekter.size)

        val inntekt = resultat.inntekter[0]
        assertEquals(1, inntekt.refusjon.size)
        val refusjonsperiode = inntekt.refusjon[0]
        assertEquals(LocalDate.of(2023, 1, 1), refusjonsperiode.fom)
        assertNull(refusjonsperiode.tom) // Tom skal være null for åpne perioder
        assertEquals(5000000L, refusjonsperiode.beløpØre)
    }

    @Test
    fun `validerer at åpen refusjonsperiode ikke krever tom`() {
        val refusjon =
            listOf(
                Refusjonsperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = null,
                    beløpØre = 5000000L,
                ),
            )

        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            beløpPerMånedØre = 5000000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = refusjon,
                        ),
                    ),
            )

        // Dette skal ikke kaste noen valideringsfeil
        val resultat = service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)
        assertNotNull(resultat)
    }

    @Test
    fun `henter eksisterende sykepengegrunnlag`() {
        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            beløpPerMånedØre = 4500000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
            )

        service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        val hentetGrunnlag = service.hentSykepengegrunnlag(periodeReferanse())

        assertNotNull(hentetGrunnlag)
        assertEquals(54000000L, hentetGrunnlag.totalInntektØre)
        assertEquals(1, hentetGrunnlag.inntekter.size)
    }

    @Test
    fun `oppdaterer eksisterende sykepengegrunnlag`() {
        val opprettRequest =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            beløpPerMånedØre = 4000000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
                begrunnelse = "Første versjon",
            )

        service.settSykepengegrunnlag(periodeReferanse(), opprettRequest, saksbehandler)

        val oppdaterRequest =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            beløpPerMånedØre = 5500000L,
                            kilde = Inntektskilde.SKJONNSFASTSETTELSE,
                            refusjon = emptyList(),
                        ),
                    ),
                begrunnelse = "Oppdatert versjon",
            )

        val oppdatertGrunnlag = service.settSykepengegrunnlag(periodeReferanse(), oppdaterRequest, saksbehandler)

        // 55 000 * 12
        assertEquals(66000000L, oppdatertGrunnlag.totalInntektØre)
        assertEquals("Oppdatert versjon", oppdatertGrunnlag.begrunnelse)
        assertEquals(1, oppdatertGrunnlag.inntekter.size)
        assertEquals(Inntektskilde.SKJONNSFASTSETTELSE, oppdatertGrunnlag.inntekter[0].kilde)
    }

    @Test
    fun `sletter sykepengegrunnlag`() {
        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            beløpPerMånedØre = 4500000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
            )

        service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // Verifiser at grunnlaget finnes
        val hentetFørSletting = service.hentSykepengegrunnlag(periodeReferanse())
        assertNotNull(hentetFørSletting)

        // Slett grunnlaget
        service.slettSykepengegrunnlag(periodeReferanse())

        // Verifiser at grunnlaget er slettet
        val hentetEtterSletting = service.hentSykepengegrunnlag(periodeReferanse())
        assertNull(hentetEtterSletting)
    }

    @Test
    fun `returnerer null for periode uten sykepengegrunnlag`() {
        val grunnlag = service.hentSykepengegrunnlag(periodeReferanse())

        assertNull(grunnlag)
    }

    @Test
    fun `beregner korrekt når totalinntekt er eksakt 6G`() {
        // Månedlig beløp som gir eksakt 6G årlig
        val eksakt6G = 78096000L / 12L

        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            beløpPerMånedØre = eksakt6G,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
            )

        val resultat = service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        assertEquals(eksakt6G * 12L, resultat.totalInntektØre)
        assertTrue(resultat.grunnbeløp6GØre == 74416800L || resultat.grunnbeløp6GØre == 78096000L)
        // Ikke over grensen
        assertEquals(false, resultat.begrensetTil6G)
        assertEquals(eksakt6G * 12L, resultat.sykepengegrunnlagØre)
    }

    @Test
    fun `kaster feil når inntekt refererer til ikke-eksisterende inntektsforhold`() {
        val ikkeEksisterendeYrkesaktivitetId = UUID.randomUUID()
        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = ikkeEksisterendeYrkesaktivitetId,
                            beløpPerMånedØre = 4500000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
            )

        val exception =
            assertThrows<InputValideringException> {
                service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)
            }

        assertTrue(exception.message!!.contains("finnes ikke på behandlingen"))
        assertTrue(exception.message!!.contains(ikkeEksisterendeYrkesaktivitetId.toString()))
    }

    @Test
    fun `kaster feil når inntektsforhold mangler inntekt i requesten`() {
        // Opprett et ekstra inntektsforhold som ikke vil ha inntekt i requesten
        val ekstraYrkesaktivitetDbRecord =
            YrkesaktivitetDbRecord(
                id = UUID.randomUUID(),
                kategorisering = frilanserKategorisering(),
                kategoriseringGenerert = null,
                dagoversikt = emptyList(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = periode.id,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )
        val yrkesaktivitetDao = YrkesaktivitetDao(dataSource)
        yrkesaktivitetDao.opprettYrkesaktivitet(
            id = ekstraYrkesaktivitetDbRecord.id,
            kategorisering = YrkesaktivitetKategoriseringMapper.fromMap(ekstraYrkesaktivitetDbRecord.kategorisering),
            dagoversikt = ekstraYrkesaktivitetDbRecord.dagoversikt,
            saksbehandlingsperiodeId = ekstraYrkesaktivitetDbRecord.saksbehandlingsperiodeId,
            opprettet = ekstraYrkesaktivitetDbRecord.opprettet,
            generertFraDokumenter = ekstraYrkesaktivitetDbRecord.generertFraDokumenter,
            perioder = ekstraYrkesaktivitetDbRecord.perioder,
        )

        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetDbRecord.id,
                            beløpPerMånedØre = 4500000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
            )

        val exception =
            assertThrows<InputValideringException> {
                service.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)
            }

        assertTrue(exception.message!!.contains("mangler inntekt i requesten"))
        assertTrue(exception.message!!.contains(ekstraYrkesaktivitetDbRecord.id.toString()))
    }
}
