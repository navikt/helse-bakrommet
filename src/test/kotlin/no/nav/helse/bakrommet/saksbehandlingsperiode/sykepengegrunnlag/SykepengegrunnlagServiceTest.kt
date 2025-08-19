package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDao
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    lateinit var service: SykepengegrunnlagService

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        val personDao = PersonDao(dataSource)
        personDao.opprettPerson(fnr, personId)
        val behandlingDao = SaksbehandlingsperiodeDao(dataSource)
        behandlingDao.opprettPeriode(periode)
        val inntektsforholdDao = InntektsforholdDao(dataSource)
        inntektsforholdDao.opprettInntektsforhold(inntektsforhold)

        val sykepengegrunnlagDao = SykepengegrunnlagDao(dataSource)
        val faktiskInntektDao = FaktiskInntektDao(dataSource)
        service = SykepengegrunnlagService(sykepengegrunnlagDao, faktiskInntektDao)
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
                faktiskeInntekter =
                    listOf(
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold.id,
                            // 45 000 kr/måned
                            beløpPerMånedØre = 4500000L,
                            kilde = Inntektskilde.SAKSBEHANDLER,
                            erSkjønnsfastsatt = false,
                            skjønnsfastsettelseBegrunnelse = null,
                            refusjon = null,
                            opprettetAv = null,
                        ),
                    ),
                begrunnelse = "Standard beregning",
            )

        val resultat = service.opprettSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // 45 000 * 12 = 540 000 kr
        assertEquals(54000000L, resultat.totalInntektØre)
        // 6G = 744 168 kr
        assertEquals(74416800L, resultat.grunnbeløp6GØre)
        assertEquals(false, resultat.begrensetTil6G)
        // Ikke begrenset
        assertEquals(54000000L, resultat.sykepengegrunnlagØre)
        assertEquals("Standard beregning", resultat.begrunnelse)
        assertEquals(saksbehandler.navIdent, resultat.opprettetAv)
        assertEquals(1, resultat.versjon)
        assertEquals(1, resultat.faktiskeInntekter.size)
        // ID skal være satt etter lagring
        assertNotNull(resultat.faktiskeInntekter[0].id)
    }

    @Test
    fun `beregner sykepengegrunnlag med 6G-begrensning`() {
        val request =
            SykepengegrunnlagRequest(
                faktiskeInntekter =
                    listOf(
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold.id,
                            // 80 000 kr/måned
                            beløpPerMånedØre = 8000000L,
                            kilde = Inntektskilde.SAKSBEHANDLER,
                            erSkjønnsfastsatt = false,
                            skjønnsfastsettelseBegrunnelse = null,
                            refusjon = null,
                            opprettetAv = null,
                        ),
                    ),
            )

        val resultat = service.opprettSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // 80 000 * 12 = 960 000 kr
        assertEquals(96000000L, resultat.totalInntektØre)
        // 6G = 744 168 kr
        assertEquals(74416800L, resultat.grunnbeløp6GØre)
        assertEquals(true, resultat.begrensetTil6G)
        // Begrenset til 6G
        assertEquals(74416800L, resultat.sykepengegrunnlagØre)
    }

    @Test
    fun `beregner sykepengegrunnlag med flere inntektsforhold`() {
        // Opprett et andre inntektsforhold
        val inntektsforhold2 =
            Inntektsforhold(
                id = UUID.randomUUID(),
                kategorisering = """{"INNTEKTSKATEGORI": "FRILANSER"}""".asJsonNode(),
                kategoriseringGenerert = null,
                dagoversikt = """[]""".asJsonNode(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = periode.id,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )
        val inntektsforholdDao = InntektsforholdDao(dataSource)
        inntektsforholdDao.opprettInntektsforhold(inntektsforhold2)

        val request =
            SykepengegrunnlagRequest(
                faktiskeInntekter =
                    listOf(
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold.id,
                            // 30 000 kr/måned
                            beløpPerMånedØre = 3000000L,
                            kilde = Inntektskilde.SAKSBEHANDLER,
                            erSkjønnsfastsatt = false,
                            skjønnsfastsettelseBegrunnelse = null,
                            refusjon = null,
                            opprettetAv = null,
                        ),
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold2.id,
                            // 25 000 kr/måned
                            beløpPerMånedØre = 2500000L,
                            kilde = Inntektskilde.AINNTEKT,
                            erSkjønnsfastsatt = false,
                            skjønnsfastsettelseBegrunnelse = null,
                            refusjon = null,
                            opprettetAv = null,
                        ),
                    ),
                begrunnelse = "Kombinert beregning",
            )

        val resultat = service.opprettSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // (30 000 + 25 000) * 12 = 660 000 kr
        assertEquals(66000000L, resultat.totalInntektØre)
        assertEquals(false, resultat.begrensetTil6G)
        assertEquals(66000000L, resultat.sykepengegrunnlagØre)
        assertEquals(2, resultat.faktiskeInntekter.size)
    }

    @Test
    fun `oppretter sykepengegrunnlag med skjønnsfastsettelse og refusjon`() {
        val refusjon =
            Refusjonsforhold(
                refusjonsbeløpPerMånedØre = 5000000L,
                refusjonsgrad = 100,
            )

        val request =
            SykepengegrunnlagRequest(
                faktiskeInntekter =
                    listOf(
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold.id,
                            // 50 000 kr/måned
                            beløpPerMånedØre = 5000000L,
                            kilde = Inntektskilde.SKJONNSFASTSETTELSE,
                            erSkjønnsfastsatt = true,
                            skjønnsfastsettelseBegrunnelse = "Fastsatt skjønnsmessig",
                            refusjon = refusjon,
                            opprettetAv = null,
                        ),
                    ),
                begrunnelse = "Skjønnsfastsettelse med refusjon",
            )

        val resultat = service.opprettSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // 50 000 * 12 = 600 000 kr
        assertEquals(60000000L, resultat.totalInntektØre)
        assertEquals(1, resultat.faktiskeInntekter.size)

        val faktiskInntekt = resultat.faktiskeInntekter[0]
        assertEquals(true, faktiskInntekt.erSkjønnsfastsatt)
        assertEquals("Fastsatt skjønnsmessig", faktiskInntekt.skjønnsfastsettelseBegrunnelse)
        assertEquals(Inntektskilde.SKJONNSFASTSETTELSE, faktiskInntekt.kilde)
        assertNotNull(faktiskInntekt.refusjon)
        faktiskInntekt.refusjon?.let { refusjon ->
            assertEquals(5000000L, refusjon.refusjonsbeløpPerMånedØre)
            assertEquals(100, refusjon.refusjonsgrad)
        }
    }

    @Test
    fun `henter eksisterende sykepengegrunnlag`() {
        val request =
            SykepengegrunnlagRequest(
                faktiskeInntekter =
                    listOf(
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold.id,
                            beløpPerMånedØre = 4500000L,
                            kilde = Inntektskilde.SAKSBEHANDLER,
                            erSkjønnsfastsatt = false,
                            skjønnsfastsettelseBegrunnelse = null,
                            refusjon = null,
                            opprettetAv = null,
                        ),
                    ),
            )

        service.opprettSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        val hentetGrunnlag = service.hentSykepengegrunnlag(periodeReferanse())

        assertNotNull(hentetGrunnlag)
        assertEquals(54000000L, hentetGrunnlag.totalInntektØre)
        assertEquals(1, hentetGrunnlag.faktiskeInntekter.size)
    }

    @Test
    fun `oppdaterer eksisterende sykepengegrunnlag`() {
        val opprettRequest =
            SykepengegrunnlagRequest(
                faktiskeInntekter =
                    listOf(
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold.id,
                            beløpPerMånedØre = 4000000L,
                            kilde = Inntektskilde.SAKSBEHANDLER,
                            erSkjønnsfastsatt = false,
                            skjønnsfastsettelseBegrunnelse = null,
                            refusjon = null,
                            opprettetAv = null,
                        ),
                    ),
                begrunnelse = "Første versjon",
            )

        service.opprettSykepengegrunnlag(periodeReferanse(), opprettRequest, saksbehandler)

        val oppdaterRequest =
            SykepengegrunnlagRequest(
                faktiskeInntekter =
                    listOf(
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold.id,
                            beløpPerMånedØre = 5500000L,
                            kilde = Inntektskilde.SKJONNSFASTSETTELSE,
                            erSkjønnsfastsatt = true,
                            skjønnsfastsettelseBegrunnelse = "Justert etter gjennomgang",
                            refusjon = null,
                            opprettetAv = null,
                        ),
                    ),
                begrunnelse = "Oppdatert versjon",
            )

        val oppdatertGrunnlag = service.oppdaterSykepengegrunnlag(periodeReferanse(), oppdaterRequest, saksbehandler)

        // 55 000 * 12
        assertEquals(66000000L, oppdatertGrunnlag.totalInntektØre)
        assertEquals("Oppdatert versjon", oppdatertGrunnlag.begrunnelse)
        assertEquals(2, oppdatertGrunnlag.versjon)
        assertEquals(1, oppdatertGrunnlag.faktiskeInntekter.size)
        assertEquals(true, oppdatertGrunnlag.faktiskeInntekter[0].erSkjønnsfastsatt)
    }

    @Test
    fun `sletter sykepengegrunnlag`() {
        val request =
            SykepengegrunnlagRequest(
                faktiskeInntekter =
                    listOf(
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold.id,
                            beløpPerMånedØre = 4500000L,
                            kilde = Inntektskilde.SAKSBEHANDLER,
                            erSkjønnsfastsatt = false,
                            skjønnsfastsettelseBegrunnelse = null,
                            refusjon = null,
                            opprettetAv = null,
                        ),
                    ),
            )

        service.opprettSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // Verifiser at grunnlaget finnes
        val hentetFørSletting = service.hentSykepengegrunnlag(periodeReferanse())
        assertNotNull(hentetFørSletting)

        // Slett grunnlaget
        service.slettSykepengegrunnlag(periodeReferanse(), saksbehandler)

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
        val eksakt6G = 74416800L / 12L

        val request =
            SykepengegrunnlagRequest(
                faktiskeInntekter =
                    listOf(
                        FaktiskInntekt(
                            id = null,
                            inntektsforholdId = inntektsforhold.id,
                            beløpPerMånedØre = eksakt6G,
                            kilde = Inntektskilde.SAKSBEHANDLER,
                            erSkjønnsfastsatt = false,
                            skjønnsfastsettelseBegrunnelse = null,
                            refusjon = null,
                            opprettetAv = null,
                        ),
                    ),
            )

        val resultat = service.opprettSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        assertEquals(eksakt6G * 12L, resultat.totalInntektØre)
        assertEquals(74416800L, resultat.grunnbeløp6GØre)
        // Ikke over grensen
        assertEquals(false, resultat.begrensetTil6G)
        assertEquals(eksakt6G * 12L, resultat.sykepengegrunnlagØre)
    }
}
