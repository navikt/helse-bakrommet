package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagBase
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.beregnSykepengegrunnlag
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.*
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.*
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Periodetype.ARBEIDSGIVERPERIODE
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.*

/**
 * Kotlin DSL for å lage testdata for utbetalingsberegning
 *
 * OPPDATERING: Denne DSL-en er oppdatert til å bruke den nye Sykepengegrunnlag-klassen
 * og beregner sykepengegrunnlaget automatisk basert på yrkesaktivitetene.
 *
 * Endringer:
 * - Lagt til skjæringstidspunkt() metode som må settes
 * - Fjernet inntekt() metode - bruk inntektData() på yrkesaktivitet i stedet
 * - Sykepengegrunnlag beregnes nå automatisk med beregnSykepengegrunnlag()
 *
 * Eksempel på bruk:
 * ```
 * val resultat = utbetalingsberegningTestOgBeregn {
 *     periode {
 *         fra dato(1.januar(2024))
 *         til dato(31.januar(2024))
 *     }
 *     skjæringstidspunkt(1.januar(2024))
 *
 *     yrkesaktivitet {
 *         som arbeidstaker("123456789")
 *         fra dato(1.januar(2024))
 *         er syk(grad = 100, antallDager = 5)
 *         med inntektData {
 *             med beløp(50000) // 50 000 kr/mnd
 *         }
 *         med refusjonsdata {
 *             med periode(1.januar(2024), 5.januar(2024), 30000) // 30 000 kr/mnd refusjon
 *         }
 *     }
 * }
 * ```
 */
class UtbetalingsberegningTestBuilder {
    private var saksbehandlingsperiode: PeriodeDto? = null
    private var arbeidsgiverperiode: PeriodeDto? = null
    private var skjæringstidspunkt: LocalDate? = null
    private val yrkesaktiviteter = mutableListOf<YrkesaktivitetBuilder>()

    fun periode(init: PeriodeBuilder.() -> Unit) {
        val builder = PeriodeBuilder()
        builder.init()
        saksbehandlingsperiode = builder.build()
    }

    fun skjæringstidspunkt(dato: LocalDate) {
        this.skjæringstidspunkt = dato
    }

    fun `med skjæringstidspunkt`(dato: LocalDate) {
        skjæringstidspunkt(dato)
    }

    fun yrkesaktivitet(init: YrkesaktivitetBuilder.() -> Unit) {
        val builder = YrkesaktivitetBuilder()
        builder.init()
        yrkesaktiviteter.add(builder)
    }

    fun build(): UtbetalingsberegningInput {
        val periode = saksbehandlingsperiode ?: throw IllegalStateException("Saksbehandlingsperiode må være satt")
        val skjæringstidspunktDato = skjæringstidspunkt ?: throw IllegalStateException("Skjæringstidspunkt må være satt")
        val yrkesaktivitetListe = yrkesaktiviteter.map { it.build(periode) }
        val sykepengegrunnlag = beregnSykepengegrunnlag(yrkesaktivitetListe, skjæringstidspunktDato)

        return UtbetalingsberegningInput(
            sykepengegrunnlag = sykepengegrunnlag,
            yrkesaktiviteter = yrkesaktivitetListe,
            saksbehandlingsperiode = periode,
            arbeidsgiverperiode = arbeidsgiverperiode,
            tilkommenInntekt = emptyList(),
            vilkår = emptyList(),
        )
    }
}

class PeriodeBuilder {
    private var fom: LocalDate? = null
    private var tom: LocalDate? = null

    fun fra(dato: LocalDate) {
        this.fom = dato
    }

    fun `fra dato`(dato: LocalDate) {
        fra(dato)
    }

    fun til(dato: LocalDate) {
        this.tom = dato
    }

    fun `til dato`(dato: LocalDate) {
        til(dato)
    }

    fun build(): PeriodeDto {
        val fomDato = fom ?: throw IllegalStateException("Periode må ha fom-dato")
        val tomDato = tom ?: throw IllegalStateException("Periode må ha tom-dato")
        return PeriodeDto(fom = fomDato, tom = tomDato)
    }
}

class YrkesaktivitetBuilder {
    private var id: UUID = UUID.randomUUID()
    private var behandlingId: UUID = UUID.randomUUID()
    private var inntektskategori: String = "ARBEIDSTAKER"
    private val kategorisering = mutableMapOf<String, String>()
    private val dagoversikt = mutableListOf<Dag>()
    private var gjeldendeDato: LocalDate? = null
    private var arbeidsgiverperiode: Pair<LocalDate, LocalDate>? = null
    private var inntektData: InntektData? = null
    private var refusjonsdata: List<Refusjonsperiode>? = null

    fun id(id: UUID) {
        this.id = id
    }

    fun behandlingId(id: UUID) {
        this.behandlingId = id
    }

    fun arbeidstaker(orgnummer: String? = null) {
        inntektskategori = "ARBEIDSTAKER"
        kategorisering["ORGNUMMER"] = orgnummer ?: "123456789"
        kategorisering["ER_SYKMELDT"] = "ER_SYKMELDT_JA"
        kategorisering["TYPE_ARBEIDSTAKER"] = "ORDINÆRT_ARBEIDSFORHOLD"
    }

    fun `som arbeidstaker`(orgnummer: String? = null) {
        arbeidstaker(orgnummer)
    }

    fun arbeidsledig() {
        inntektskategori = "ARBEIDSLEDIG"
    }

    fun `som arbeidsledig`() {
        arbeidsledig()
    }

    fun inaktiv(variant: String = "INAKTIV_VARIANT_A") {
        inntektskategori = "INAKTIV"
        kategorisering["VARIANT_AV_INAKTIV"] = variant
    }

    fun `som inaktiv`(variant: String = "INAKTIV_VARIANT_A") {
        inaktiv(variant)
    }

    fun næringsdrivende(forsikringstype: String = "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG") {
        inntektskategori = "SELVSTENDIG_NÆRINGSDRIVENDE"
        kategorisering["ER_SYKMELDT"] = "ER_SYKMELDT_JA"
        kategorisering["TYPE_SELVSTENDIG_NÆRINGSDRIVENDE"] = "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE"
        kategorisering["SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING"] = forsikringstype
    }

    fun `som næringsdrivende`(forsikringstype: String = "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG") {
        næringsdrivende(forsikringstype)
    }

    fun fra(dato: LocalDate) {
        gjeldendeDato = dato
    }

    fun `fra dato`(dato: LocalDate) {
        fra(dato)
    }

    fun arbeidsgiverperiode(init: PeriodeBuilder.() -> Unit) {
        val builder = PeriodeBuilder()
        builder.init()
        val periode = builder.build()
        arbeidsgiverperiode = Pair(periode.fom, periode.tom)
    }

    fun `med arbeidsgiverperiode`(init: PeriodeBuilder.() -> Unit) {
        arbeidsgiverperiode(init)
    }

    fun inntektData(data: InntektData) {
        this.inntektData = data
    }

    fun `med inntektData`(init: InntektDataBuilder.() -> Unit) {
        val builder = InntektDataBuilder()
        builder.init()
        this.inntektData = builder.build()
    }

    fun `med refusjonsdata`(init: RefusjonsdataBuilder.() -> Unit) {
        val builder = RefusjonsdataBuilder()
        builder.init()
        this.refusjonsdata = builder.build()
    }

    fun syk(
        grad: Int = 100,
        antallDager: Int = 1,
    ) {
        repeat(antallDager) {
            dagoversikt.add(
                Dag(
                    dato = gjeldendeDato!!,
                    dagtype = Dagtype.Syk,
                    grad = grad,
                    avslåttBegrunnelse = emptyList(),
                    kilde = Kilde.Saksbehandler,
                ),
            )
            gjeldendeDato = gjeldendeDato!!.plusDays(1)
        }
    }

    fun `er syk`(
        grad: Int = 100,
        antallDager: Int = 1,
    ) {
        syk(grad, antallDager)
    }

    fun sykNav(
        grad: Int = 100,
        antallDager: Int = 1,
    ) {
        repeat(antallDager) {
            dagoversikt.add(
                Dag(
                    dato = gjeldendeDato!!,
                    dagtype = Dagtype.SykNav,
                    grad = grad,
                    avslåttBegrunnelse = emptyList(),
                    kilde = Kilde.Saksbehandler,
                ),
            )
            gjeldendeDato = gjeldendeDato!!.plusDays(1)
        }
    }

    fun `er syk nav`(
        grad: Int = 100,
        antallDager: Int = 1,
    ) {
        sykNav(grad, antallDager)
    }

    fun arbeidsdag(antallDager: Int = 1) {
        repeat(antallDager) {
            dagoversikt.add(
                Dag(
                    dato = gjeldendeDato!!,
                    dagtype = Dagtype.Arbeidsdag,
                    grad = null,
                    avslåttBegrunnelse = emptyList(),
                    kilde = Kilde.Saksbehandler,
                ),
            )
            gjeldendeDato = gjeldendeDato!!.plusDays(1)
        }
    }

    fun `har arbeidsdager`(antallDager: Int = 1) {
        arbeidsdag(antallDager)
    }

    fun ferie(antallDager: Int = 1) {
        repeat(antallDager) {
            dagoversikt.add(
                Dag(
                    dato = gjeldendeDato!!,
                    dagtype = Dagtype.Ferie,
                    grad = null,
                    avslåttBegrunnelse = emptyList(),
                    kilde = Kilde.Saksbehandler,
                ),
            )
            gjeldendeDato = gjeldendeDato!!.plusDays(1)
        }
    }

    fun `har ferie`(antallDager: Int = 1) {
        ferie(antallDager)
    }

    fun permisjon(antallDager: Int = 1) {
        repeat(antallDager) {
            dagoversikt.add(
                Dag(
                    dato = gjeldendeDato!!,
                    dagtype = Dagtype.Permisjon,
                    grad = null,
                    avslåttBegrunnelse = emptyList(),
                    kilde = Kilde.Saksbehandler,
                ),
            )
            gjeldendeDato = gjeldendeDato!!.plusDays(1)
        }
    }

    fun `har permisjon`(antallDager: Int = 1) {
        permisjon(antallDager)
    }

    fun avslått(
        begrunnelse: List<String> = emptyList(),
        antallDager: Int = 1,
    ) {
        repeat(antallDager) {
            dagoversikt.add(
                Dag(
                    dato = gjeldendeDato!!,
                    dagtype = Dagtype.Avslått,
                    grad = null,
                    avslåttBegrunnelse = begrunnelse,
                    kilde = Kilde.Saksbehandler,
                ),
            )
            gjeldendeDato = gjeldendeDato!!.plusDays(1)
        }
    }

    fun `er avslått`(
        begrunnelse: List<String> = emptyList(),
        antallDager: Int = 1,
    ) {
        avslått(begrunnelse, antallDager)
    }

    fun andreYtelser(
        begrunnelse: List<String> = emptyList(),
        antallDager: Int = 1,
    ) {
        repeat(antallDager) {
            dagoversikt.add(
                Dag(
                    dato = gjeldendeDato!!,
                    dagtype = Dagtype.AndreYtelser,
                    grad = null,
                    andreYtelserBegrunnelse = begrunnelse,
                    kilde = Kilde.Saksbehandler,
                ),
            )
            gjeldendeDato = gjeldendeDato!!.plusDays(1)
        }
    }

    fun `har andre ytelser`(
        begrunnelse: List<String> = emptyList(),
        antallDager: Int = 1,
    ) {
        andreYtelser(begrunnelse, antallDager)
    }

    fun build(saksbehandlingsperiode: PeriodeDto): Yrkesaktivitetsperiode {
        val perioder =
            arbeidsgiverperiode?.let { (fom, tom) ->
                Perioder(
                    type = ARBEIDSGIVERPERIODE,
                    perioder = listOf(Periode(fom = fom, tom = tom)),
                )
            }

        kategorisering["INNTEKTSKATEGORI"] = inntektskategori

        // Fyll ut manglende dager som arbeidsdager
        val fullstendigDagoversikt = fyllUtManglendeDagerSomArbeidsdager(dagoversikt, saksbehandlingsperiode)

        // Konverter kategorisering til YrkesaktivitetKategorisering
        val yrkesaktivitetKategorisering = lagYrkesaktivitetKategorisering()

        val sykdomstidlinje =
            fullstendigDagoversikt
                .filter {
                    it.dagtype != Dagtype.Avslått
                }.toMutableList()
        val avslattTidlinje =
            fullstendigDagoversikt
                .filter {
                    it.dagtype == Dagtype.Avslått
                }.also { dager ->
                    dager.forEach {
                        // Legg til en arbeidsdag i sykdomstidlinjen for hver avslått dag. Denne hacken kan fjernes om vi forbedrer DSLen ved å skille på sykdom og avslag
                        sykdomstidlinje.add(
                            Dag(
                                dato = it.dato,
                                dagtype = Dagtype.Syk,
                                grad = 100,
                                avslåttBegrunnelse = emptyList(),
                                kilde = Kilde.Saksbehandler,
                            ),
                        )
                    }
                }

        return Yrkesaktivitetsperiode(
            id = YrkesaktivitetsperiodeId(id),
            kategorisering = yrkesaktivitetKategorisering,
            kategoriseringGenerert = null,
            dagoversikt = Dagoversikt(sykdomstidlinje, avslattTidlinje),
            dagoversiktGenerert = null,
            behandlingId = BehandlingId(behandlingId),
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = emptyList(),
            perioder = perioder,
            inntektRequest = null,
            inntektData = inntektData,
            refusjon = refusjonsdata,
        )
    }

    private fun lagYrkesaktivitetKategorisering(): YrkesaktivitetKategorisering =
        when (inntektskategori) {
            "ARBEIDSTAKER" -> {
                val orgnummer = kategorisering["ORGNUMMER"] ?: "123456789"
                val erSykmeldt = kategorisering["ER_SYKMELDT"] == "ER_SYKMELDT_JA"

                YrkesaktivitetKategorisering.Arbeidstaker(
                    sykmeldt = erSykmeldt,
                    typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = orgnummer),
                )
            }

            "ARBEIDSLEDIG" -> {
                YrkesaktivitetKategorisering.Arbeidsledig()
            }

            "INAKTIV" -> {
                YrkesaktivitetKategorisering.Inaktiv()
            }

            "SELVSTENDIG_NÆRINGSDRIVENDE" -> {
                val erSykmeldt = kategorisering["ER_SYKMELDT"] == "ER_SYKMELDT_JA"
                val forsikring =
                    when (kategorisering["SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING"]) {
                        "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG" -> SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
                        "FORSIKRING_100_PROSENT_FRA_17_SYKEDAG" -> SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG
                        "INGEN_FORSIKRING" -> SelvstendigForsikring.INGEN_FORSIKRING
                        else -> SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG
                    }
                val type =
                    when (kategorisering["TYPE_SELVSTENDIG_NÆRINGSDRIVENDE"]) {
                        "ORDINÆR_SELVSTENDIG_NÆRINGSDRIVENDE" -> TypeSelvstendigNæringsdrivende.Ordinær(forsikring)
                        else -> TypeSelvstendigNæringsdrivende.Ordinær(forsikring)
                    }
                YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                    sykmeldt = erSykmeldt,
                    typeSelvstendigNæringsdrivende = type,
                )
            }

            else -> {
                YrkesaktivitetKategorisering.Arbeidstaker(
                    sykmeldt = true,
                    typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                )
            }
        }
}

// Hjelpeklasse for å lage InntektData
class InntektDataBuilder {
    private var yrkesaktivitetId: UUID = UUID.randomUUID()
    private var beløpPerMåned: Int = 5000000 // 50 000 kr/mnd

    fun yrkesaktivitetId(id: UUID) {
        this.yrkesaktivitetId = id
    }

    fun beløp(krPerMåned: Int) {
        this.beløpPerMåned = krPerMåned
    }

    fun `med beløp`(krPerMåned: Int) {
        beløp(krPerMåned)
    }

    fun build(): InntektData {
        val årligInntekt = InntektbeløpDto.Årlig((beløpPerMåned * 12).toDouble())
        return InntektData.ArbeidstakerAinntekt(
            omregnetÅrsinntekt = årligInntekt.tilInntekt(),
            kildedata = emptyMap(),
        )
    }
}

// Hjelpeklasse for å lage Refusjonsdata
class RefusjonsdataBuilder {
    private val refusjonsperioder = mutableListOf<Refusjonsperiode>()

    fun periode(
        fom: LocalDate,
        tom: LocalDate? = null,
        beløp: Int, // kr per måned
    ) {
        val månedligBeløp = Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(beløp.toDouble()))
        refusjonsperioder.add(
            Refusjonsperiode(
                fom = fom,
                tom = tom,
                beløp = månedligBeløp,
            ),
        )
    }

    fun `med periode`(
        fom: LocalDate,
        tom: LocalDate? = null,
        beløp: Int,
    ) {
        periode(fom, tom, beløp)
    }

    fun periode(
        fom: LocalDate,
        tom: LocalDate? = null,
        beløpØre: Long, // øre per måned
    ) {
        val månedligBeløp = Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(beløpØre.toDouble() / 100.0))
        refusjonsperioder.add(
            Refusjonsperiode(
                fom = fom,
                tom = tom,
                beløp = månedligBeløp,
            ),
        )
    }

    fun build(): List<Refusjonsperiode> = refusjonsperioder.toList()
}

// Extension function for å lage InntektData
fun inntektData(init: InntektDataBuilder.() -> Unit): InntektData {
    val builder = InntektDataBuilder()
    builder.init()
    return builder.build()
}

// Extension functions for enklere bruk
fun utbetalingsberegningTestdata(init: UtbetalingsberegningTestBuilder.() -> Unit): UtbetalingsberegningInput {
    val builder = UtbetalingsberegningTestBuilder()
    builder.init()
    return builder.build()
}

/**
 * Helper-funksjon som kaller beregnUtbetalingerForAlleYrkesaktiviteter og byggOppdragFraBeregning
 */
fun beregnOgByggOppdrag(
    input: UtbetalingsberegningInput,
    ident: String = "01019012345",
): BeregningResultat {
    val beregnet = beregnUtbetalingerForAlleYrkesaktiviteter(input)
    val oppdrag = byggOppdragFraBeregning(beregnet, input.yrkesaktiviteter, NaturligIdent(ident))
    return BeregningResultat(beregnet, oppdrag, input.sykepengegrunnlag)
}

/**
 * Kombinert DSL som setter opp testdata og beregner resultatet i ett steg
 */
fun utbetalingsberegningTestOgBeregn(
    ident: String = "01019012345",
    init: UtbetalingsberegningTestBuilder.() -> Unit,
): BeregningResultat {
    val input = utbetalingsberegningTestdata(init)
    return beregnOgByggOppdrag(input, ident)
}

/**
 * Data class for å holde beregning og oppdrag sammen
 */
data class BeregningResultat(
    val beregnet: List<YrkesaktivitetUtbetalingsberegning>,
    val oppdrag: List<Oppdrag>,
    val sykepengegrunnlag: SykepengegrunnlagBase,
)

// Hjelpefunksjoner for å lage sykepengegrunnlag
private fun fyllUtManglendeDagerSomArbeidsdager(
    eksisterendeDager: List<Dag>,
    saksbehandlingsperiode: PeriodeDto,
): List<Dag> {
    val dagerMap = eksisterendeDager.associateBy { it.dato }.toMutableMap()

    var aktuellDato = saksbehandlingsperiode.fom
    while (!aktuellDato.isAfter(saksbehandlingsperiode.tom)) {
        if (!dagerMap.containsKey(aktuellDato)) {
            dagerMap[aktuellDato] =
                Dag(
                    dato = aktuellDato,
                    dagtype = Dagtype.Arbeidsdag,
                    grad = null,
                    avslåttBegrunnelse = emptyList(),
                    kilde = Kilde.Saksbehandler,
                )
        }
        aktuellDato = aktuellDato.plusDays(1)
    }

    return dagerMap.values.sortedBy { it.dato }
}

/**
 * DSL for å asserte beregning og oppdrag resultater
 */
class BeregningAssertionBuilder(
    private val resultat: BeregningResultat,
) {
    fun haYrkesaktivitet(
        yrkesaktivitetId: UUID,
        init: YrkesaktivitetAssertionBuilder.() -> Unit,
    ) {
        val yrkesaktivitetResultat =
            resultat.beregnet.find { it.yrkesaktivitetId == yrkesaktivitetId }
        assertNotNull(yrkesaktivitetResultat, "Fant ikke yrkesaktivitet med id $yrkesaktivitetId")

        val builder = YrkesaktivitetAssertionBuilder(yrkesaktivitetResultat)
        builder.init()
    }

    fun `ha yrkesaktivitet`(
        yrkesaktivitetId: UUID,
        init: YrkesaktivitetAssertionBuilder.() -> Unit,
    ) {
        haYrkesaktivitet(yrkesaktivitetId, init)
    }

    fun haOppdrag(init: OppdragAssertionBuilder.() -> Unit) {
        val builder = OppdragAssertionBuilder(resultat.oppdrag)
        builder.init()
    }

    fun `har oppdrag`(init: OppdragAssertionBuilder.() -> Unit) {
        haOppdrag(init)
    }
}

class YrkesaktivitetAssertionBuilder(
    private val yrkesaktivitetResultat: YrkesaktivitetUtbetalingsberegning,
) {
    fun harAntallDager(antall: Int) {
        val faktiskAntall = yrkesaktivitetResultat.utbetalingstidslinje.size
        assertEquals(antall, faktiskAntall, "Forventet $antall dager, men fikk $faktiskAntall dager")
    }

    fun `skal ha antall dager`(antall: Int) {
        harAntallDager(antall)
    }

    fun harDekningsgrad(grad: Int) {
        val deknignsgrad = yrkesaktivitetResultat.dekningsgrad?.verdi?.prosentDesimal ?: 0.0
        assertEquals(grad, (deknignsgrad * 100).toInt(), "Forventet $grad, men fikk dekningsgrad $deknignsgrad")
    }

    fun `skal ha dekningsgrad`(grad: Int) {
        harDekningsgrad(grad)
    }

    fun harDekningsgradBegrunnelse(begrunnelse: BeregningskoderDekningsgrad) {
        val sporing = yrkesaktivitetResultat.dekningsgrad?.sporing
        assertEquals(begrunnelse, sporing, "Forventet $begrunnelse, men fikk $sporing")
    }

    fun `skal ha dekningsgrad begrunnelse`(begrunnelse: BeregningskoderDekningsgrad) {
        harDekningsgradBegrunnelse(begrunnelse)
    }

    fun dag(
        dato: LocalDate,
        init: DagAssertionBuilder.() -> Unit,
    ) {
        val dag =
            yrkesaktivitetResultat.utbetalingstidslinje.find { it.dato == dato }
        assertNotNull(dag, "Fant ikke dag for dato $dato")

        val builder = DagAssertionBuilder(dag)
        builder.init()
    }

    fun `på dato`(
        dato: LocalDate,
        init: DagAssertionBuilder.() -> Unit,
    ) {
        dag(dato, init)
    }
}

class DagAssertionBuilder(
    private val dag: no.nav.helse.utbetalingstidslinje.Utbetalingsdag,
) {
    fun harTotalGrad(grad: Int) {
        val faktiskGrad = dag.økonomi.totalSykdomsgrad.toDouble()
        assertEquals(grad.toDouble(), faktiskGrad, "Forventet totalgrad $grad, men fikk $faktiskGrad for dato ${dag.dato}")
    }

    fun `skal ha total grad`(grad: Int) {
        harTotalGrad(grad)
    }

    fun harSykdomsGrad(grad: Int) {
        val faktiskGrad =
            (
                dag.økonomi.sykdomsgrad
                    .dto()
                    .prosentDesimal
            ) * 100
        assertEquals(grad, faktiskGrad.toInt(), "Forventet grad $grad, men fikk $faktiskGrad for dato ${dag.dato}")
    }

    fun `skal ha sykdoms grad`(grad: Int) {
        harSykdomsGrad(grad)
    }

    fun harIngenUtbetaling() {
        val harUtbetaling = dag.økonomi.personbeløp != null && dag.økonomi.personbeløp!!.dagligInt > 0
        assertFalse(harUtbetaling, "Forventet ingen utbetaling for dato ${dag.dato}, men fikk utbetaling")
    }

    fun `skal ha ingen utbetaling`() {
        harIngenUtbetaling()
    }

    fun harUtbetaling(beløp: Int) {
        val faktiskUtbetaling = dag.økonomi.personbeløp?.dagligInt
        assertEquals(beløp, faktiskUtbetaling, "Forventet utbetaling $beløp for dato ${dag.dato}, men fikk $faktiskUtbetaling")
    }

    fun `skal ha utbetaling`(beløp: Int) {
        harUtbetaling(beløp)
    }

    fun harRefusjon(beløp: Int? = null) {
        val harRefusjon = dag.økonomi.arbeidsgiverbeløp != null && dag.økonomi.arbeidsgiverbeløp!!.dagligInt > 0
        assertTrue(harRefusjon, "Forventet refusjon for dato ${dag.dato}, men fikk ingen refusjon")
        if (beløp != null) {
            val faktiskRefusjon = dag.økonomi.arbeidsgiverbeløp?.dagligInt
            assertEquals(beløp, faktiskRefusjon, "Forventet refusjon $beløp for dato ${dag.dato}, men fikk $faktiskRefusjon")
        }
    }

    fun `skal ha refusjon`(beløp: Int? = null) {
        harRefusjon(beløp)
    }

    fun harIngenRefusjon() {
        val harRefusjon = dag.økonomi.arbeidsgiverbeløp != null && dag.økonomi.arbeidsgiverbeløp!!.dagligInt > 0
        assertFalse(harRefusjon, "Forventet ingen refusjon for dato ${dag.dato}, men fikk refusjon")
    }

    fun `skal ha ingen refusjon`() {
        harIngenRefusjon()
    }
}

class OppdragAssertionBuilder(
    private val oppdrag: List<Oppdrag>,
) {
    fun harAntallOppdrag(antall: Int) {
        val faktiskAntall = oppdrag.size
        assertEquals(antall, faktiskAntall, "Forventet $antall oppdrag, men fikk $faktiskAntall oppdrag.")
    }

    fun `skal ha antall oppdrag`(antall: Int) {
        harAntallOppdrag(antall)
    }

    fun oppdrag(
        index: Int = 0,
        init: OppdragMatcherBuilder.() -> Unit,
    ) {
        if (index >= oppdrag.size) {
            fail("Oppdrag $index finnes ikke. Total antall oppdrag: ${oppdrag.size}")
        }

        val builder = OppdragMatcherBuilder(oppdrag[index])
        builder.init()
    }

    fun `oppdrag nummer`(
        index: Int = 0,
        init: OppdragMatcherBuilder.() -> Unit,
    ) {
        oppdrag(index, init)
    }
}

class OppdragMatcherBuilder(
    private val oppdrag: Oppdrag,
) {
    fun harMottaker(mottaker: String) {
        assertEquals(mottaker, oppdrag.mottaker, "Forventet mottaker $mottaker, men fikk ${oppdrag.mottaker}")
    }

    fun `skal ha mottaker`(mottaker: String) {
        harMottaker(mottaker)
    }

    fun harNettoBeløp(beløp: Int) {
        assertEquals(beløp, oppdrag.nettoBeløp, "Forventet netto beløp $beløp, men fikk ${oppdrag.nettoBeløp}")
    }

    fun `skal ha netto beløp`(beløp: Int) {
        harNettoBeløp(beløp)
    }

    fun harTotalbeløp(beløp: Int) {
        assertEquals(beløp, oppdrag.totalbeløp(), "Forventet netto beløp $beløp, men fikk ${oppdrag.totalbeløp()}")
    }

    fun `skal ha total beløp`(beløp: Int) {
        harTotalbeløp(beløp)
    }

    fun harFagområde(fagområde: String) {
        assertEquals(fagområde, oppdrag.fagområde.verdi, "Forventet fagområde $fagområde, men fikk ${oppdrag.fagområde.verdi}")
    }

    fun `skal ha fagområde`(fagområde: String) {
        harFagområde(fagområde)
    }
}

// Extension functions for enklere bruk av matcher-DSL
fun BeregningResultat.skal(init: BeregningAssertionBuilder.() -> Unit) {
    val builder = BeregningAssertionBuilder(this)
    builder.init()
}
