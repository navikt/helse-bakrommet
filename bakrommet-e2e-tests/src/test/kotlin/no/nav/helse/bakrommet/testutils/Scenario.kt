package no.nav.helse.bakrommet.testutils

import io.ktor.client.request.get
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.Inntekt
import no.nav.helse.bakrommet.ainntekt.InntektApiUt
import no.nav.helse.bakrommet.ainntekt.Inntektsinformasjon
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.BeregningResponseUtDto
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentSykepengegrunnlag
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.oppdaterInntekt
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettArbeidstakerYrkesaktivitet
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettSaksbehandlingsperiode
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settDagoversikt
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settSkjaeringstidspunkt
import no.nav.helse.bakrommet.util.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object ScenarioDefaults {
    val skjæringstidspunkt = LocalDate.parse("2024-05-17")
    val fom = LocalDate.parse("2024-05-17")
    val tom = fom.plusDays(14)
}

data class ScenarioResultat(
    val periode: Saksbehandlingsperiode,
    val sykepengegrunnlag: Sykepengegrunnlag,
    val utbetalingsberegning: BeregningResponseUtDto?,
) {
    infix fun `skal ha sykepengegrunnlag`(beløp: Double) {
        assertEquals(beløp, sykepengegrunnlag.sykepengegrunnlag.beløp)
    }
}

data class Scenario(
    val fnr: String = "01019011111",
    val personId: String = "abcde",
    val skjæringstidspunkt: LocalDate = ScenarioDefaults.skjæringstidspunkt,
    val fom: LocalDate = ScenarioDefaults.fom,
    val tom: LocalDate = ScenarioDefaults.tom,
    val yrkesaktiviteter: List<YA>,
) {
    fun run(
        testBlock: (suspend ApplicationTestBuilder.(daoer: Daoer, resultat: ScenarioResultat) -> Unit)? = null,
    ) {
        val ainntekt828 =
            InntektApiUt(
                data =
                    yrkesaktiviteter.filter { it.inntekt is AInntekt }.flatMap {
                        (it.inntekt as AInntekt).lagDelsvar(this@Scenario, it.orgnr)
                    },
            )

        runApplicationTest(
            /*inntektsmeldingClient =
                InntektsmeldingApiMock.inntektsmeldingClientMock(
                    configuration = TestOppsett.configuration.inntektsmelding,
                    oboClient = TestOppsett.oboClient,
                    mockClient =
                        inntektsmeldingMockHttpClient(
                            configuration = TestOppsett.configuration.inntektsmelding,
                            oboClient = TestOppsett.oboClient,
                            fnrTilSvar = mapOf(FNR to "[$im1]"),
                            inntektsmeldingIdTilSvar = mapOf(im1Id to im1),
                            callCounter = antallKallTilInntektsmeldingAPI,
                        ),
                ),*/
            aInntektClient =
                AInntektMock.aInntektClientMock(
                    fnrTilSvar = mapOf(fnr to ainntekt828.serialisertTilString()),
                ),
            // sigrunClient = SigrunClientTest.client2010to2050(FNR),
        ) { daoer ->
            daoer.personDao.opprettPerson(fnr, personId)

            val periode = opprettSaksbehandlingsperiode(personId, fom, tom)

            if (skjæringstidspunkt != fom) {
                // Sett skjæringstidspunkt for perioden via action
                settSkjaeringstidspunkt(personId, periode.id, skjæringstidspunkt)
            }

            val yaMedId =
                yrkesaktiviteter.map { ya ->
                    ya to
                        when (ya.type) {
                            YAType.ARBTAKER -> opprettArbeidstakerYrkesaktivitet(periode.id, personId = personId, orgnr = ya.orgnr)
                        }
                }

            yaMedId.forEach { (ya, yrkesaktivitetId) ->
                // Oppdater inntekt via action
                oppdaterInntekt(personId, periode.id, yrkesaktivitetId, ya.inntekt.request)
            }

            yaMedId.forEach { (ya, yrkesaktivitetId) ->
                if (ya.dagoversikt != null) {
                    settDagoversikt(
                        periodeId = periode.id,
                        yrkesaktivitetId = yrkesaktivitetId,
                        personId = personId,
                        dager = ya.dagoversikt.lagDagListe(fom = periode.fom, tom = periode.tom),
                    )
                }
            }

            // Hent sykepengegrunnlag via action
            val sykepengegrunnlag = hentSykepengegrunnlag(periode.spilleromPersonId, periode.id)

            val beregning = hentUtbetalingsberegning(periode.id, periode.spilleromPersonId)

            if (testBlock != null) {
                testBlock.invoke(
                    this,
                    daoer,
                    ScenarioResultat(
                        periode = periode,
                        sykepengegrunnlag = sykepengegrunnlag.sykepengegrunnlag!!,
                        utbetalingsberegning = beregning,
                    ),
                )
            }
        }
    }
}

enum class YAType {
    ARBTAKER,
}

data class YA(
    val type: YAType,
    val orgnr: String,
    val inntekt: YAInntekt,
    val dagoversikt: YADagoversikt? = null,
)

sealed class YADagoversikt {
    abstract fun lagDagListe(
        fom: LocalDate,
        tom: LocalDate,
    ): List<Dag>
}

class SykAlleDager : YADagoversikt() {
    override fun lagDagListe(
        fom: LocalDate,
        tom: LocalDate,
    ): List<Dag> =
        fom
            .datesUntil(tom.plusDays(1))
            .map { dato ->
                Dag(
                    dato = dato,
                    dagtype = Dagtype.Syk,
                    grad = 100,
                    avslåttBegrunnelse = listOf(),
                    andreYtelserBegrunnelse = listOf(),
                    kilde = Kilde.Søknad,
                )
            }.toList()
}

sealed class YAInntekt {
    abstract val request: InntektRequest
}

class AInntekt(
    vararg måneder: Int,
) : YAInntekt() {
    private val måneder = måneder.toList()

    fun lagDelsvar(
        scenario: Scenario,
        orgnr: String,
    ): List<Inntektsinformasjon> =
        måneder
            .reversed()
            .mapIndexed { i, beløp ->
                Inntektsinformasjon(
                    maaned = YearMonth.from(scenario.skjæringstidspunkt).minusMonths(i + 1L),
                    underenhet = orgnr,
                    opplysningspliktig = "OP$orgnr",
                    inntektListe =
                        listOf(
                            Inntekt(
                                type = "LOENNSINNTEKT",
                                beloep = BigDecimal.valueOf(beløp.toLong()),
                            ),
                        ),
                )
            }.reversed()

    override val request =
        InntektRequest.Arbeidstaker(
            data =
                ArbeidstakerInntektRequest.Ainntekt(
                    begrunnelse = "Velger inntektsmelding for arbeidstaker",
                    refusjon = emptyList(),
                ),
        )
}
