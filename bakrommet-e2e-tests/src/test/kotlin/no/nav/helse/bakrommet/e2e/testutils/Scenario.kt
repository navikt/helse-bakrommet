package no.nav.helse.bakrommet.e2e.testutils

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.SammenlikningsgrunnlagDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.SykepengegrunnlagBaseDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.SykepengegrunnlagDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.BeregningResponseDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.e2e.*
import no.nav.helse.bakrommet.e2e.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import no.nav.helse.bakrommet.infrastruktur.provider.AInntektResponse
import no.nav.helse.bakrommet.infrastruktur.provider.Inntekt
import no.nav.helse.bakrommet.infrastruktur.provider.Inntektsinformasjon
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.inntektsmeldingMockHttpClient
import no.nav.helse.bakrommet.inntektsmelding.skapInntektsmelding
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sigrun.SigrunMock.sigrunErrorResponse
import no.nav.helse.bakrommet.sigrun.sigrunÅr
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendMock
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.mai
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.*
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as InntektsmeldingKontrakt

object ScenarioDefaults {
    val skjæringstidspunkt = 17.mai(2024)
    val fom = 17.mai(2024)
    val tom = fom.plusDays(13)
}

fun BeregningResponseDto?.direkteTotalbeløp(fnr: String): Int =
    this
        ?.beregningData
        ?.spilleromOppdrag
        ?.oppdrag
        ?.filter { it.mottaker == fnr }
        ?.sumOf { it.totalbeløp } ?: 0

data class ScenarioData(
    val scenario: Scenario,
    val behandling: BehandlingDto,
    val sykepengegrunnlag: SykepengegrunnlagBaseDto?,
    val sammenlikningsgrunnlag: SammenlikningsgrunnlagDto?,
    val yrkesaktiviteter: List<YrkesaktivitetDto>,
    val utbetalingsberegning: BeregningResponseDto?,
    val beslutterToken: String,
) {
    fun `skal ha sykepengegrunnlag`(beløp: Double) {
        assertEquals(beløp, sykepengegrunnlag!!.sykepengegrunnlag)
    }

    fun `skal ha nærings del`(beløp: Double) {
        require(sykepengegrunnlag is SykepengegrunnlagDto)
        assertEquals(beløp, sykepengegrunnlag.næringsdel!!.næringsdel)
    }

    fun `skal ikke ha sammenlikningsgrunnlag`() {
        assertNull(sammenlikningsgrunnlag, "Forventet at sammenlikningsgrunnlag er null")
    }

    fun `skal ha direkteutbetaling`(beløp: Int) {
        val totalDirekte = utbetalingsberegning.direkteTotalbeløp(scenario.fnr)

        assertEquals(beløp, totalDirekte, "Feil direkteutbetaling i oppdraget")
    }

    fun `skal ha dagtype`(
        expectedDagtype: DagtypeDto,
        dato: LocalDate,
    ) {
        val dagoversikt =
            yrkesaktiviteter
                .flatMap { it.dagoversikt ?: emptyList() }
        val dag = dagoversikt.firstOrNull { it.dato == dato }

        assertTrue(dag != null, "Fant ikke dag for dato $dato")
        assertEquals(expectedDagtype, dag!!.dagtype, "Feil dagtype for dato $dato, forventet $expectedDagtype, fikk ${dag.dagtype}")
    }

    fun `skal ha klassekode`(klassekode: Klassekode) {
        val klassekoder =
            utbetalingsberegning
                ?.beregningData
                ?.spilleromOppdrag
                ?.oppdrag
                ?.filter { it.mottaker == scenario.fnr }
                ?.flatMap { it.linjer }
                ?.map { it.klassekode }

        assertTrue(klassekoder?.all { it == klassekode.verdi } == true, "Feil klassekode i oppdraget")
        assertTrue(klassekoder?.isNotEmpty() == true, "Ingen klassekoder funnet for mottaker ${scenario.fnr}")
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

    fun `næringsdrivende yrkesaktivitet`(): YrkesaktivitetDto =
        yrkesaktiviteter
            .first { it.kategorisering is YrkesaktivitetKategoriseringDto.SelvstendigNæringsdrivende }

    fun `arbeidstaker yrkesaktivitet`(orgnummer: String): YrkesaktivitetDto {
        val arbeidstaker =
            yrkesaktiviteter
                .filter { it.kategorisering is YrkesaktivitetKategoriseringDto.Arbeidstaker }
                .first {
                    val k = it.kategorisering as YrkesaktivitetKategoriseringDto.Arbeidstaker
                    when (val type = k.typeArbeidstaker) {
                        is TypeArbeidstakerDto.Ordinær -> type.orgnummer == orgnummer
                        is TypeArbeidstakerDto.Maritim -> type.orgnummer == orgnummer
                        is TypeArbeidstakerDto.Fisker -> type.orgnummer == orgnummer
                        else -> false
                    }
                }
        return arbeidstaker
    }
}

infix fun YrkesaktivitetDto.harBeregningskode(expectedKode: BeregningskoderSykepengegrunnlag) {
    val beregningskodeActual = this.inntektData?.sporing
    assertEquals(expectedKode.name, beregningskodeActual, "Feil beregningskode for yrkesaktivitet ${this.id}")
}

data class Scenario(
    val yrkesaktiviteter: List<YA>,
    val fnr: String = enNaturligIdent().value,
    val skjæringstidspunkt: LocalDate = ScenarioDefaults.skjæringstidspunkt,
    val fom: LocalDate = ScenarioDefaults.fom,
    val tom: LocalDate = ScenarioDefaults.tom,
    val besluttOgGodkjenn: Boolean = true,
    val soknader: List<SykepengesoknadDTO>? = null,
    val vilkår: List<VilkaarsvurderingDto>? = null,
) {
    lateinit var pseudoId: UUID
        private set

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
            AInntektResponse(
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
            sykepengesøknadProvider =
                SykepengesoknadBackendMock.sykepengesoknadMock(
                    configuration = TestOppsett.configuration.sykepengesoknadBackend,
                    tokenUtvekslingProvider = TestOppsett.oboClient,
                    fnrTilSoknader = mapOf(fnr to (soknader ?: emptyList())),
                ),
            inntektsmeldingClient =
                InntektsmeldingApiMock.inntektsmeldingClientMock(
                    configuration = TestOppsett.configuration.inntektsmelding,
                    tokenUtvekslingProvider = TestOppsett.oboClient,
                    mockClient =
                        inntektsmeldingMockHttpClient(
                            configuration = TestOppsett.configuration.inntektsmelding,
                            fnrTilInntektsmeldinger = mapOf(fnr to inntektsmeldinger),
                        ),
                ),
            pensjonsgivendeInntektProvider = SigrunMock.sigrunMockClient(fnrÅrTilSvar = sigrunsvar),
            aInntektClient =
                AInntektMock.aInntektClientMock(
                    fnrTilAInntektResponse = mapOf(fnr to ainntekt828),
                ),
        ) {
            pseudoId = personsøk(NaturligIdent(fnr))

            val periode =
                opprettBehandlingOgForventOk(
                    pseudoId,
                    fom = fom,
                    tom = tom,
                    søknader = soknader?.map { UUID.fromString(it.id) },
                )
            val beslutterToken = oAuthMock.token(navIdent = "B111111", grupper = listOf("GRUPPE_BESLUTTER"))

            if (skjæringstidspunkt != fom) {
                // Sett skjæringstidspunkt for perioden via action
                settSkjaeringstidspunkt(pseudoId.toString(), periode.id, skjæringstidspunkt)
            }

            val yaMedId =
                yrkesaktiviteter.map { ya ->
                    ya to
                        when (ya) {
                            is Arbeidstaker -> {
                                // sjekk om vi har en som passer fra før
                                val eksisterendeYa =
                                    hentYrkesaktiviteter(pseudoId, periode.id)
                                        .filter {
                                            it.kategorisering is YrkesaktivitetKategoriseringDto.Arbeidstaker
                                        }.firstOrNull { it.kategorisering.maybeOrgnummer() == ya.orgnr }

                                eksisterendeYa?.id
                                    ?: opprettYrkesaktivitetOld(
                                        personId = pseudoId,
                                        periode.id,
                                        YrkesaktivitetKategoriseringDto.Arbeidstaker(
                                            sykmeldt = true,
                                            typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = ya.orgnr),
                                        ),
                                    )
                            }

                            is Selvstendig -> {
                                // sjekk om vi har en som passer fra før
                                val eksisterendeYa =
                                    hentYrkesaktiviteter(pseudoId, periode.id)
                                        .firstOrNull {
                                            it.kategorisering is YrkesaktivitetKategoriseringDto.SelvstendigNæringsdrivende
                                        }

                                eksisterendeYa?.id
                                    ?: opprettYrkesaktivitetOld(
                                        personId = pseudoId,
                                        periode.id,
                                        YrkesaktivitetKategoriseringDto.SelvstendigNæringsdrivende(
                                            sykmeldt = true,
                                            typeSelvstendigNæringsdrivende =
                                                TypeSelvstendigNæringsdrivendeDto.Ordinær(
                                                    forsikring = ya.forsikring,
                                                ),
                                        ),
                                    )
                            }
                        }
                }

            yaMedId.forEach { (ya, yrkesaktivitetId) ->
                // Oppdater inntekt via action
                if (ya.inntekt != null) {
                    oppdaterInntekt(pseudoId, periode.id, yrkesaktivitetId, ya.inntekt.request)
                }
            }

            yaMedId.forEach { (ya, yrkesaktivitetId) ->
                if (ya.dagoversikt != null) {
                    settDagoversikt(
                        personId = pseudoId,
                        behandlingId = periode.id,
                        yrkesaktivitetId = yrkesaktivitetId,
                        dager = ya.dagoversikt.lagDagListe(fom = periode.fom, tom = periode.tom),
                    )
                }
            }

            vilkår?.forEach { vilkårDto ->
                oppdaterVilkårsvurdering(
                    personId = pseudoId,
                    behandlingId = periode.id,
                    vilkår = vilkårDto,
                )
            }
            // Hent sykepengegrunnlag via action
            val sykepengegrunnlag = hentSykepengegrunnlag(pseudoId, periode.id)

            val beregning = hentUtbetalingsberegning(pseudoId, periode.id)
            val yrkesaktiviteter = hentYrkesaktiviteter(pseudoId, periode.id)
            val reloadedPeriode = hentAlleBehandlinger(pseudoId).first { it.id == periode.id }
            if (besluttOgGodkjenn) {
                sendTilBeslutning(pseudoId, reloadedPeriode.id)
                taTilBesluting(pseudoId, reloadedPeriode.id, beslutterToken)
                godkjennOgForventOk(pseudoId, reloadedPeriode.id, beslutterToken)
            }
            testBlock?.invoke(
                this,
                ScenarioData(
                    behandling = reloadedPeriode,
                    sykepengegrunnlag = sykepengegrunnlag?.sykepengegrunnlag,
                    sammenlikningsgrunnlag = sykepengegrunnlag?.sammenlikningsgrunnlag,
                    utbetalingsberegning = beregning,
                    yrkesaktiviteter = yrkesaktiviteter,
                    beslutterToken = beslutterToken,
                    scenario = this@Scenario,
                ),
            )
        }
    }
}

sealed class YA(
    val inntekt: YAInntekt?,
    val dagoversikt: YADagoversikt? = null,
)

class Arbeidstaker(
    val orgnr: String,
    inntekt: YAInntekt? = null,
    dagoversikt: YADagoversikt? = null,
) : YA(inntekt, dagoversikt)

class Selvstendig(
    inntekt: YAInntekt,
    dagoversikt: YADagoversikt? = null,
    val forsikring: SelvstendigForsikringDto = SelvstendigForsikringDto.INGEN_FORSIKRING,
) : YA(inntekt, dagoversikt)

sealed class YADagoversikt {
    abstract fun lagDagListe(
        fom: LocalDate,
        tom: LocalDate,
    ): List<DagDto>
}

class SykAlleDager : YADagoversikt() {
    override fun lagDagListe(
        fom: LocalDate,
        tom: LocalDate,
    ): List<DagDto> = lagSykedager(fom, tom, grad = 100)
}

class GradertSyk(
    val grad: Int,
) : YADagoversikt() {
    override fun lagDagListe(
        fom: LocalDate,
        tom: LocalDate,
    ): List<DagDto> = lagSykedager(fom, tom, grad = grad)
}

fun lagSykedager(
    fom: LocalDate,
    tom: LocalDate,
    grad: Int,
): List<DagDto> =
    fom
        .datesUntil(tom.plusDays(1))
        .map { dato ->
            DagDto(
                dato = dato,
                dagtype = DagtypeDto.Syk,
                grad = grad,
                avslåttBegrunnelse = listOf(),
                andreYtelserBegrunnelse = listOf(),
                kilde = KildeDto.Søknad,
            )
        }.toList()

sealed class YAInntekt {
    abstract val request: InntektRequestDto
}

class Inntektsmelding(
    val beregnetInntekt: Double,
    val arbeidsgiverperioder: List<Periode> = emptyList(),
    val refusjon: RefusjonsperiodeDto? = null,
) : YAInntekt() {
    fun skapInntektsmelding(
        fnr: String,
        orgnr: String,
    ): InntektsmeldingKontrakt =
        skapInntektsmelding(
            arbeidsgiverperioder = arbeidsgiverperioder,
            inntektsmeldingId = inntektmeldingid.toString(),
            arbeidstakerFnr = fnr,
            virksomhetsnummer = orgnr,
            månedsinntekt = beregnetInntekt,
        )

    val inntektmeldingid = UUID.randomUUID()

    override val request: InntektRequestDto =
        InntektRequestDto.Arbeidstaker(
            data =
                ArbeidstakerInntektRequestDto.Inntektsmelding(
                    begrunnelse = "Velger inntektsmelding for arbeidstaker",
                    inntektsmeldingId = inntektmeldingid.toString(),
                    refusjon = refusjon?.let { listOf(it) },
                ),
        )
}

class SkjønnsfastsattManglendeRapportering(
    årsinntekt: Double,
    begrunnelse: String = "Fordi arbeidsgiver har ikke rapportert inntekten på vanlig måte",
) : YAInntekt() {
    override val request: InntektRequestDto =
        InntektRequestDto.Arbeidstaker(
            data =
                ArbeidstakerInntektRequestDto.Skjønnsfastsatt(
                    begrunnelse = begrunnelse,
                    årsinntekt = årsinntekt,
                    årsak = ArbeidstakerSkjønnsfastsettelseÅrsakDto.MANGELFULL_RAPPORTERING,
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
        InntektRequestDto.Arbeidstaker(
            data =
                ArbeidstakerInntektRequestDto.Ainntekt(
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
        InntektRequestDto.SelvstendigNæringsdrivende(
            data =
                PensjonsgivendeInntektRequestDto.PensjonsgivendeInntekt(
                    begrunnelse = "8-35",
                ),
        )
}
