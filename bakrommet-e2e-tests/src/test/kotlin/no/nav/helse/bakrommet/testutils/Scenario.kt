package no.nav.helse.bakrommet.testutils

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.Inntekt
import no.nav.helse.bakrommet.ainntekt.InntektApiUt
import no.nav.helse.bakrommet.ainntekt.Inntektsinformasjon
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.enInntektsmelding
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.inntektsmeldingMockHttpClient
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.BeregningResponseUtDto
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDTO
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentSykepengegrunnlag
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.oppdaterInntekt
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettArbeidstakerYrkesaktivitet
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettSaksbehandlingsperiode
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settDagoversikt
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settSkjaeringstidspunkt
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.mai
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object ScenarioDefaults {
    val skjæringstidspunkt = 17.mai(2024)
    val fom = 17.mai(2024)
    val tom = fom.plusDays(13)
}

data class ScenarioData(
    val scenario: Scenario,
    val periode: Saksbehandlingsperiode,
    val sykepengegrunnlag: Sykepengegrunnlag,
    val yrkesaktiviteter: List<YrkesaktivitetDTO>,
    val utbetalingsberegning: BeregningResponseUtDto?,
    val daoer: Daoer,
) {
    infix fun `skal ha sykepengegrunnlag`(beløp: Double) {
        assertEquals(beløp, sykepengegrunnlag.sykepengegrunnlag.beløp)
    }

    infix fun `skal ha utbetaling`(beløp: Int) {
        val nettoDirekte =
            utbetalingsberegning
                ?.beregningData
                ?.oppdrag
                ?.filter { it.mottaker == scenario.fnr }
                ?.sumOf { it.nettoBeløp } ?: 0

        assertEquals(beløp, nettoDirekte, "Feil nettobeløp i utbetalingsberegning")
    }

    fun `skal ha refusjon`(
        beløp: Int,
        orgnummer: String,
    ) {
        val nettoRefusjon =
            utbetalingsberegning
                ?.beregningData
                ?.oppdrag
                ?.filter { it.mottaker == orgnummer }
                ?.sumOf { it.nettoBeløp } ?: 0

        assertEquals(beløp, nettoRefusjon)
    }

    infix fun `arbeidstaker yrkesaktivitet`(orgnummer: String): YrkesaktivitetDTO =
        yrkesaktiviteter
            .filter { it.kategorisering["INNTEKTSKATEGORI"] == "ARBEIDSTAKER" }
            .first { it.kategorisering["ORGNUMMER"] == orgnummer }
}

infix fun YrkesaktivitetDTO.harBeregningskode(expectedKode: String) {
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
) {
    fun run(
        testBlock: (suspend ApplicationTestBuilder.(resultat: ScenarioData) -> Unit)? = null,
    ) {
        val ainntekt828 =
            InntektApiUt(
                data =
                    yrkesaktiviteter.filter { it.inntekt is AInntekt }.flatMap {
                        (it.inntekt as AInntekt).lagDelsvar(this@Scenario, it.orgnr)
                    },
            )

        val inntektmeldinger =
            yrkesaktiviteter
                .filter { it.inntekt is Inntektsmelding }
                .map { (it.inntekt as Inntektsmelding).skapInntektsmelding(fnr, it.orgnr) }

        runApplicationTest(
            inntektsmeldingClient =
                InntektsmeldingApiMock.inntektsmeldingClientMock(
                    configuration = TestOppsett.configuration.inntektsmelding,
                    oboClient = TestOppsett.oboClient,
                    mockClient =
                        inntektsmeldingMockHttpClient(
                            configuration = TestOppsett.configuration.inntektsmelding,
                            oboClient = TestOppsett.oboClient,
                            fnrTilSvar =
                                mapOf(
                                    fnr to
                                        inntektmeldinger.joinToString(
                                            ",",
                                            prefix = "[",
                                            postfix = "]",
                                        ) { it.second },
                                ),
                            inntektsmeldingIdTilSvar = inntektmeldinger.toMap().mapKeys { (key) -> key.toString() },
                        ),
                ),
            aInntektClient =
                AInntektMock.aInntektClientMock(
                    fnrTilSvar = mapOf(fnr to ainntekt828.serialisertTilString()),
                ),
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
                            YAType.ARBTAKER ->
                                opprettArbeidstakerYrkesaktivitet(
                                    periode.id,
                                    personId = personId,
                                    orgnr = ya.orgnr,
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
            val yrkesaktiviteter = hentYrkesaktiviteter(periode.id, periode.spilleromPersonId)
            if (testBlock != null) {
                testBlock.invoke(
                    this,
                    ScenarioData(
                        periode = periode,
                        sykepengegrunnlag = sykepengegrunnlag.sykepengegrunnlag!!,
                        utbetalingsberegning = beregning,
                        yrkesaktiviteter = yrkesaktiviteter,
                        daoer = daoer,
                        scenario = this@Scenario,
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

class Inntektsmelding(
    val beregnetInntekt: Double,
    vararg val refusjon: Refusjonsperiode,
) : YAInntekt() {
    fun skapInntektsmelding(
        fnr: String,
        orgnr: String,
    ): Pair<UUID, String> =
        Pair(
            inntektmeldingid,
            enInntektsmelding(
                arbeidstakerFnr = fnr,
                virksomhetsnummer = orgnr,
                inntektsmeldingId = inntektmeldingid.toString(),
                beregnetInntekt = beregnetInntekt,
            ),
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
