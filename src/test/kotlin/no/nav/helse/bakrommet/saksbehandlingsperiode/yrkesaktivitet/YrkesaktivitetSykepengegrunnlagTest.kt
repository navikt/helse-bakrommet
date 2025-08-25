package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.*
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class YrkesaktivitetSykepengegrunnlagTest {
    private val dataSource = TestDataSource.dbModule.dataSource
    private val fnr = "01019012345"
    private val personId = "0h0a1"
    private val saksbehandler = Bruker("ABC", "A. B. C", "ola@nav.no", emptySet())
    private val periode =
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

    private lateinit var inntektsforholdService: InntektsforholdService
    private lateinit var sykepengegrunnlagService: SykepengegrunnlagService

    @BeforeEach
    fun setOpp() {
        TestDataSource.resetDatasource()
        val personDao = PersonDao(dataSource)
        personDao.opprettPerson(fnr, personId)
        val behandlingDao = SaksbehandlingsperiodeDao(dataSource)
        behandlingDao.opprettPeriode(periode)

        val yrkesaktivitetDao = YrkesaktivitetDao(dataSource)
        val sykepengegrunnlagDao = SykepengegrunnlagDao(dataSource)

        val beregningDao = UtbetalingsberegningDao(dataSource)

        inntektsforholdService =
            InntektsforholdService(
                object : InntektsforholdServiceDaoer {
                    override val saksbehandlingsperiodeDao = behandlingDao
                    override val yrkesaktivitetDao = yrkesaktivitetDao
                    override val sykepengegrunnlagDao = sykepengegrunnlagDao
                    override val beregningDao = beregningDao
                },
                TransactionalSessionFactory(dataSource) { session ->
                    object : InntektsforholdServiceDaoer {
                        override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDao(session)
                        override val yrkesaktivitetDao = YrkesaktivitetDao(session)
                        override val sykepengegrunnlagDao = SykepengegrunnlagDao(session)
                        override val beregningDao = UtbetalingsberegningDao(session)
                    }
                },
            )

        sykepengegrunnlagService =
            SykepengegrunnlagService(
                object : SykepengegrunnlagServiceDaoer {
                    override val saksbehandlingsperiodeDao = behandlingDao
                    override val yrkesaktivitetDao = yrkesaktivitetDao
                    override val sykepengegrunnlagDao = sykepengegrunnlagDao
                    override val beregningDao = beregningDao
                },
                TransactionalSessionFactory(dataSource) { session ->
                    object : SykepengegrunnlagServiceDaoer {
                        override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDao(session)
                        override val yrkesaktivitetDao = YrkesaktivitetDao(session)
                        override val sykepengegrunnlagDao = SykepengegrunnlagDao(session)
                        override val beregningDao = UtbetalingsberegningDao(session)
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
    fun `sykepengegrunnlag slettes når inntektsforhold opprettes`() {
        // Given - opprett inntektsforhold først
        val kategorisering = """{"INNTEKTSKATEGORI": "ARBEIDSTAKER"}""".asJsonNode()
        val inntektsforhold = inntektsforholdService.opprettInntektsforhold(periodeReferanse(), kategorisering, saksbehandler)

        // Opprett sykepengegrunnlag
        val request =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        Inntekt(
                            inntektsforholdId = inntektsforhold.id,
                            // 5000 kr/måned
                            beløpPerMånedØre = 500000L,
                            kilde = Inntektskilde.INNTEKTSMELDING,
                            refusjon = emptyList(),
                        ),
                    ),
                begrunnelse = "Test",
            )
        sykepengegrunnlagService.settSykepengegrunnlag(periodeReferanse(), request, saksbehandler)

        // Verify sykepengegrunnlag eksisterer
        val grunnlagFør = sykepengegrunnlagService.hentSykepengegrunnlag(periodeReferanse())
        assertNotNull(grunnlagFør)

        // When - opprett nytt inntektsforhold
        val nyKategorisering = """{"INNTEKTSKATEGORI": "FRILANSER"}""".asJsonNode()
        inntektsforholdService.opprettInntektsforhold(periodeReferanse(), nyKategorisering, saksbehandler)

        // Then - sykepengegrunnlag skal være slettet
        val grunnlagEtter = sykepengegrunnlagService.hentSykepengegrunnlag(periodeReferanse())
        assertNull(grunnlagEtter)
    }
}
