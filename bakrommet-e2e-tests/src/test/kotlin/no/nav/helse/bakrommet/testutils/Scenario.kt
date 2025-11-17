package no.nav.helse.bakrommet.testutils

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.Inntekt
import no.nav.helse.bakrommet.ainntekt.InntektApiUt
import no.nav.helse.bakrommet.ainntekt.Inntektsinformasjon
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.dagoversikt.Kilde
import no.nav.helse.bakrommet.behandling.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.ArbeidstakerSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.PensjonsgivendeInntektRequest
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.BeregningResponseUtDto
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDTO
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.SelvstendigForsikring
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.TypeArbeidstaker
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.TypeSelvstendigNæringsdrivende
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.maybeOrgnummer
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.inntektsmeldingMockHttpClient
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.sendTilBeslutning
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sigrun.SigrunMock.sigrunErrorResponse
import no.nav.helse.bakrommet.sigrun.sigrunÅr
import no.nav.helse.bakrommet.taTilBesluting
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.godkjenn
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentAllePerioder
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentSykepengegrunnlag
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.oppdaterInntekt
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettSaksbehandlingsperiode
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettYrkesaktivitet
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settDagoversikt
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settSkjaeringstidspunkt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.mai
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as InntektsmeldingKontrakt

object ScenarioDefaults {
    val skjæringstidspunkt = 17.mai(2024)
    val fom = 17.mai(2024)
    val tom = fom.plusDays(13)
}

data class ScenarioData(
    val scenario: Scenario,
    val periode: Behandling,
    val sykepengegrunnlag: Sykepengegrunnlag,
    val sammenlikningsgrunnlag: Sammenlikningsgrunnlag?,
    val yrkesaktiviteter: List<YrkesaktivitetDTO>,
    val utbetalingsberegning: BeregningResponseUtDto?,
    val daoer: Daoer,
    val beslutterToken: String,
) {
    fun `skal ha sykepengegrunnlag`(beløp: Double) {
        assertEquals(beløp, sykepengegrunnlag.sykepengegrunnlag.beløp)
    }

    fun `skal ha nærings del`(beløp: Double) {
        assertEquals(beløp, sykepengegrunnlag.næringsdel!!.næringsdel.beløp)
    }

    fun `skal ikke ha sammenlikningsgrunnlag`() {
        assertNull(sammenlikningsgrunnlag, "Forventet at sammenlikningsgrunnlag er null")
    }

    fun `skal ha utbetaling`(beløp: Int) {
        val nettoDirekte =
            utbetalingsberegning
                ?.beregningData
                ?.spilleromOppdrag
                ?.oppdrag
                ?.filter { it.mottaker == scenario.fnr }
                ?.sumOf { it.totalbeløp } ?: 0

        assertEquals(beløp, nettoDirekte, "Feil nettobeløp i utbetalingsberegning")
    }

    fun `skal ha refusjon`(
        beløp: Int,
        orgnummer: String,
    ) {
        val nettoRefusjon =
            utbetalingsberegning
                ?.beregningData
                ?.spilleromOppdrag
                ?.oppdrag
                ?.filter { it.mottaker == orgnummer }
                ?.sumOf { it.totalbeløp } ?: 0

        assertEquals(beløp, nettoRefusjon)
    }

    fun `næringsdrivende yrkesaktivitet`(): YrkesaktivitetDTO =
        yrkesaktiviteter
            .first { it.kategorisering is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende }

    fun `arbeidstaker yrkesaktivitet`(orgnummer: String): YrkesaktivitetDTO =
        yrkesaktiviteter
            .filter { it.kategorisering is YrkesaktivitetKategorisering.Arbeidstaker }
            .first { (it.kategorisering as YrkesaktivitetKategorisering.Arbeidstaker).maybeOrgnummer() == orgnummer }
}

infix fun YrkesaktivitetDTO.harBeregningskode(expectedKode: BeregningskoderSykepengegrunnlag) {
    val beregningskodeActual = this.inntektData?.sporing
    assertEquals(expectedKode, beregningskodeActual, "Feil beregningskode for yrkesaktivitet ${this.id}")
}

data class Scenario(
    val yrkesaktiviteter: List<YA>,
    val fnr: String = "01019011111",
    val personId: String = "abcde",
    val skjæringstidspunkt: LocalDate = ScenarioDefaults.skjæringstidspunkt,
    val fom: LocalDate = ScenarioDefaults.fom,
    val tom: LocalDate = ScenarioDefaults.tom,
    val besluttOgGodkjenn: Boolean = true,
) {
    fun run(
        testBlock: ScenarioData.() -> Unit,
    ) {
        runWithApplicationTestBuilder { resultat ->
            testBlock(resultat)
        }
    }

    fun runWithApplicationTestBuilder(
        testBlock: (suspend ApplicationTestBuilder.(resultat: ScenarioData) -> Unit)? = null,
    ) {
        val ainntekt828 =
            InntektApiUt(
                data =
                    yrkesaktiviteter
                        .filter { it is Arbeidstaker }
                        .map { it as Arbeidstaker }
                        .filter { it.inntekt is AInntekt }
                        .flatMap {
                            (it.inntekt as AInntekt).lagDelsvar(this@Scenario, it.orgnr)
                        },
            )

        val inntektsmeldinger =
            yrkesaktiviteter
                .filter { it is Arbeidstaker }
                .map { it as Arbeidstaker }
                .filter { it.inntekt is Inntektsmelding }
                .map { (it.inntekt as Inntektsmelding).skapInntektsmelding(fnr, (it).orgnr) }

        val sigrunsvar =
            yrkesaktiviteter
                .filter { it.inntekt is SigrunInntekt }
                .map { it.inntekt as SigrunInntekt }
                .map { it.lagSigrunSvar(this@Scenario) }
                .firstOrNull() ?: emptyMap()

        runApplicationTest(
            inntektsmeldingClient =
                InntektsmeldingApiMock.inntektsmeldingClientMock(
                    configuration = TestOppsett.configuration.inntektsmelding,
                    oboClient = TestOppsett.oboClient,
                    mockClient =
                        inntektsmeldingMockHttpClient(
                            configuration = TestOppsett.configuration.inntektsmelding,
                            oboClient = TestOppsett.oboClient,
                            fnrTilInntektsmeldinger = mapOf(fnr to inntektsmeldinger),
                        ),
                ),
            sigrunClient = SigrunMock.sigrunMockClient(fnrÅrTilSvar = sigrunsvar),
            aInntektClient =
                AInntektMock.aInntektClientMock(
                    fnrTilInntektApiUt = mapOf(fnr to ainntekt828),
                ),
        ) { daoer ->
            daoer.personDao.opprettPerson(fnr, personId)

            val periode = opprettSaksbehandlingsperiode(personId, fom, tom)
            val beslutterToken = oAuthMock.token(navIdent = "B111111", grupper = listOf("GRUPPE_BESLUTTER"))

            if (skjæringstidspunkt != fom) {
                // Sett skjæringstidspunkt for perioden via action
                settSkjaeringstidspunkt(personId, periode.id, skjæringstidspunkt)
            }

            val yaMedId =
                yrkesaktiviteter.map { ya ->
                    ya to
                        when (ya) {
                            is Arbeidstaker ->
                                opprettYrkesaktivitet(
                                    personId = personId,
                                    periode.id,
                                    YrkesaktivitetKategorisering.Arbeidstaker(
                                        sykmeldt = true,
                                        typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = ya.orgnr),
                                    ),
                                )

                            is Selvstendig ->
                                opprettYrkesaktivitet(
                                    personId = personId,
                                    periode.id,
                                    YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                                        sykmeldt = true,
                                        typeSelvstendigNæringsdrivende =
                                            TypeSelvstendigNæringsdrivende.Ordinær(
                                                forsikring = ya.forsikring,
                                            ),
                                    ),
                                )
                        }
                }

            yaMedId.forEach { (ya, yrkesaktivitetId) ->
                // Oppdater inntekt via action
                oppdaterInntekt(personId, periode.id, yrkesaktivitetId, ya.inntekt.request)
            }

            yaMedId.forEach { (ya, yrkesaktivitetId) ->
                if (ya.dagoversikt != null) {
                    settDagoversikt(
                        personId = personId,
                        periodeId = periode.id,
                        yrkesaktivitetId = yrkesaktivitetId,
                        dager = ya.dagoversikt.lagDagListe(fom = periode.fom, tom = periode.tom),
                    )
                }
            }

            // Hent sykepengegrunnlag via action
            val sykepengegrunnlag = hentSykepengegrunnlag(periode.spilleromPersonId, periode.id)

            val beregning = hentUtbetalingsberegning(periode.spilleromPersonId, periode.id)
            val yrkesaktiviteter = hentYrkesaktiviteter(periode.spilleromPersonId, periode.id)
            if (besluttOgGodkjenn) {
                sendTilBeslutning(periode)
                taTilBesluting(periode, beslutterToken)
                godkjenn(periode, beslutterToken)
            }
            val reloadedPeriode = hentAllePerioder(personId).first { it.id == periode.id }
            if (testBlock != null) {
                testBlock.invoke(
                    this,
                    ScenarioData(
                        periode = reloadedPeriode,
                        sykepengegrunnlag = sykepengegrunnlag!!.sykepengegrunnlag!!,
                        sammenlikningsgrunnlag = sykepengegrunnlag.sammenlikningsgrunnlag,
                        utbetalingsberegning = beregning,
                        yrkesaktiviteter = yrkesaktiviteter,
                        daoer = daoer,
                        beslutterToken = beslutterToken,
                        scenario = this@Scenario,
                    ),
                )
            }
        }
    }
}

sealed class YA(
    val inntekt: YAInntekt,
    val dagoversikt: YADagoversikt? = null,
)

class Arbeidstaker(
    val orgnr: String,
    inntekt: YAInntekt,
    dagoversikt: YADagoversikt? = null,
) : YA(inntekt, dagoversikt)

class Selvstendig(
    inntekt: YAInntekt,
    dagoversikt: YADagoversikt? = null,
    val forsikring: SelvstendigForsikring = SelvstendigForsikring.INGEN_FORSIKRING,
) : YA(inntekt, dagoversikt)

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
    ): List<Dag> = lagSykedager(fom, tom, grad = 100)
}

fun lagSykedager(
    fom: LocalDate,
    tom: LocalDate,
    grad: Int,
): List<Dag> =
    fom
        .datesUntil(tom.plusDays(1))
        .map { dato ->
            Dag(
                dato = dato,
                dagtype = Dagtype.Syk,
                grad = grad,
                avslåttBegrunnelse = listOf(),
                andreYtelserBegrunnelse = listOf(),
                kilde = Kilde.Søknad,
            )
        }.toList()

sealed class YAInntekt {
    abstract val request: InntektRequest
}

class Inntektsmelding(
    val beregnetInntekt: Double,
    vararg val refusjon: Refusjonsperiode,
) : YAInntekt() {
    fun skapInntektsmelding(
        fnr: String,
        orgnr: String,
    ): InntektsmeldingKontrakt =
        skapInntektsmelding(
            inntektsmeldingId = inntektmeldingid.toString(),
            arbeidstakerFnr = fnr,
            virksomhetsnummer = orgnr,
            månedsinntekt = beregnetInntekt,
        )

    val inntektmeldingid = UUID.randomUUID()

    override val request: InntektRequest =
        InntektRequest.Arbeidstaker(
            data =
                ArbeidstakerInntektRequest.Inntektsmelding(
                    begrunnelse = "Velger inntektsmelding for arbeidstaker",
                    inntektsmeldingId = inntektmeldingid.toString(),
                    refusjon = refusjon.toList(),
                ),
        )
}

class SkjønnsfastsattManglendeRapportering(
    årsinntekt: Double,
    begrunnelse: String = "Fordi arbeidsgiver har ikke rapportert inntekten på vanlig måte",
) : YAInntekt() {
    override val request: InntektRequest =
        InntektRequest.Arbeidstaker(
            data =
                ArbeidstakerInntektRequest.Skjønnsfastsatt(
                    begrunnelse = begrunnelse,
                    årsinntekt = InntektbeløpDto.Årlig(årsinntekt),
                    årsak = ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING,
                ),
        )
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

class SigrunInntekt(
    vararg år: Int?,
) : YAInntekt() {
    private val årene = år.toList()

    fun lagSigrunSvar(
        scenario: Scenario,
    ): Map<Pair<String, Year>, String> {
        val åreneMedInntekt =
            årene
                .reversed()
                .mapIndexed(fun(
                    i: Int,
                    beløp: Int?,
                ): Pair<Pair<String, Year>, String> {
                    val året = Year.of(scenario.skjæringstidspunkt.year - i - 1)
                    if (beløp == null) {
                        return (scenario.fnr to året) to sigrunErrorResponse(status = 404, kode = "PGIF-008")
                    }

                    return (scenario.fnr to året) to sigrunÅr(fnr = scenario.fnr, år = året, næring = beløp)
                })
                .reversed()

        return mapOf(
            *åreneMedInntekt.toTypedArray(),
        )
    }

    override val request =
        InntektRequest.SelvstendigNæringsdrivende(
            data =
                PensjonsgivendeInntektRequest.PensjonsgivendeInntekt(
                    begrunnelse = "8-35",
                ),
        )
}
