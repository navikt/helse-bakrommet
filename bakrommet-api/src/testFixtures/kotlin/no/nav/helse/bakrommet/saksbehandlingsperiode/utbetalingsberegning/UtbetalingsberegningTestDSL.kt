package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Refusjonsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.utbetalingslinjer.Oppdrag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Kotlin DSL for å lage testdata for utbetalingsberegning
 */
class UtbetalingsberegningTestBuilder {
    private var saksbehandlingsperiode: PeriodeDto? = null
    private var arbeidsgiverperiode: PeriodeDto? = null
    private val yrkesaktiviteter = mutableListOf<YrkesaktivitetBuilder>()
    private val inntekter = mutableListOf<InntektBuilder>()

    fun periode(init: PeriodeBuilder.() -> Unit) {
        val builder = PeriodeBuilder()
        builder.init()
        saksbehandlingsperiode = builder.build()
    }

    fun arbeidsgiverperiode(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        arbeidsgiverperiode = PeriodeDto(fom = fom, tom = tom)
    }

    fun yrkesaktivitet(init: YrkesaktivitetBuilder.() -> Unit) {
        val builder = YrkesaktivitetBuilder()
        builder.init()
        yrkesaktiviteter.add(builder)
    }

    fun inntekt(init: InntektBuilder.() -> Unit) {
        val builder = InntektBuilder()
        builder.init()
        inntekter.add(builder)
    }

    fun build(): UtbetalingsberegningInput {
        val periode = saksbehandlingsperiode ?: throw IllegalStateException("Saksbehandlingsperiode må være satt")
        val yrkesaktivitetListe = yrkesaktiviteter.map { it.build(periode) }
        val sykepengegrunnlag = lagSykepengegrunnlag(inntekter.map { it.build() })

        return UtbetalingsberegningInput(
            sykepengegrunnlag = sykepengegrunnlag,
            yrkesaktivitet = yrkesaktivitetListe,
            saksbehandlingsperiode = periode,
            arbeidsgiverperiode = arbeidsgiverperiode,
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
    private var saksbehandlingsperiodeId: UUID = UUID.randomUUID()
    private var inntektskategori: String = "ARBEIDSTAKER"
    private val kategorisering = mutableMapOf<String, String>()
    private val dagoversikt = mutableListOf<Dag>()
    private var gjeldendeDato: LocalDate? = null
    private var arbeidsgiverperiode: Pair<LocalDate, LocalDate>? = null

    fun id(id: UUID) {
        this.id = id
    }

    fun saksbehandlingsperiodeId(id: UUID) {
        this.saksbehandlingsperiodeId = id
    }

    fun arbeidstaker(orgnummer: String? = null) {
        inntektskategori = "ARBEIDSTAKER"
        orgnummer?.let {
            kategorisering["ORGNUMMER"] = it
        }
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

    fun build(saksbehandlingsperiode: PeriodeDto): Yrkesaktivitet {
        val perioder =
            arbeidsgiverperiode?.let { (fom, tom) ->
                Perioder(
                    type = Periodetype.ARBEIDSGIVERPERIODE,
                    perioder = listOf(PeriodeDto(fom = fom, tom = tom)),
                )
            }

        kategorisering["INNTEKTSKATEGORI"] = inntektskategori

        // Fyll ut manglende dager som arbeidsdager
        val fullstendigDagoversikt = fyllUtManglendeDagerSomArbeidsdager(dagoversikt, saksbehandlingsperiode)

        return Yrkesaktivitet(
            id = id,
            kategorisering = kategorisering,
            kategoriseringGenerert = null,
            dagoversikt = fullstendigDagoversikt,
            dagoversiktGenerert = null,
            saksbehandlingsperiodeId = saksbehandlingsperiodeId,
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = emptyList(),
            perioder = perioder,
        )
    }
}

class InntektBuilder {
    private var yrkesaktivitetId: UUID = UUID.randomUUID()
    private var beløpPerMånedØre: Long = 5000000L // 50 000 kr/mnd
    private var kilde: Inntektskilde = Inntektskilde.AINNTEKT
    private val refusjon = mutableListOf<Refusjonsperiode>()

    fun yrkesaktivitetId(id: UUID) {
        this.yrkesaktivitetId = id
    }

    fun beløp(krPerMåned: Int) {
        this.beløpPerMånedØre = krPerMåned * 100L
    }

    fun `med beløp`(krPerMåned: Int) {
        beløp(krPerMåned)
    }

    fun beløpØre(ørePerMåned: Long) {
        this.beløpPerMånedØre = ørePerMåned
    }

    fun `med beløp i øre`(ørePerMåned: Long) {
        beløpØre(ørePerMåned)
    }

    fun kilde(kilde: Inntektskilde) {
        this.kilde = kilde
    }

    fun `fra kilde`(kilde: Inntektskilde) {
        this.kilde = kilde
    }

    fun refusjon(init: RefusjonBuilder.() -> Unit) {
        val builder = RefusjonBuilder()
        builder.init()
        refusjon.add(builder.build())
    }

    fun `med refusjon`(init: RefusjonBuilder.() -> Unit) {
        refusjon(init)
    }

    fun build(): Inntekt {
        return Inntekt(
            yrkesaktivitetId = yrkesaktivitetId,
            beløpPerMånedØre = beløpPerMånedØre,
            kilde = kilde,
            refusjon = refusjon,
        )
    }
}

class RefusjonBuilder {
    private var fom: LocalDate? = null
    private var tom: LocalDate? = null
    private var beløpØre: Long = 0L

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

    fun åpen() {
        this.tom = null
    }

    fun `er åpen`() {
        åpen()
    }

    fun beløp(krPerMåned: Int) {
        this.beløpØre = krPerMåned * 100L
    }

    fun `med beløp`(krPerMåned: Int) {
        beløp(krPerMåned)
    }

    fun beløpØre(ørePerMåned: Long) {
        this.beløpØre = ørePerMåned
    }

    fun `med beløp i øre`(ørePerMåned: Long) {
        beløpØre(ørePerMåned)
    }

    fun build(): Refusjonsperiode {
        return Refusjonsperiode(
            fom = fom ?: throw IllegalStateException("Refusjonsperiode må ha fom-dato"),
            tom = tom,
            beløpØre = beløpØre,
        )
    }
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
    ident: String = "TESTIDENT",
): BeregningResultat {
    val beregnet = beregnUtbetalingerForAlleYrkesaktiviteter(input)
    val oppdrag = byggOppdragFraBeregning(beregnet, input.yrkesaktivitet, ident)
    return BeregningResultat(beregnet, oppdrag)
}

/**
 * Kombinert DSL som setter opp testdata og beregner resultatet i ett steg
 */
fun utbetalingsberegningTestOgBeregn(
    ident: String = "TESTIDENT",
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

private fun lagSykepengegrunnlag(inntekter: List<Inntekt>): SykepengegrunnlagResponse {
    val totalInntektØre = 12 * inntekter.sumOf { it.beløpPerMånedØre }
    val grunnbeløpØre = 10000000L
    val grunnbeløp6GØre = 6 * grunnbeløpØre

    return SykepengegrunnlagResponse(
        id = UUID.randomUUID(),
        saksbehandlingsperiodeId = UUID.randomUUID(),
        inntekter = inntekter,
        totalInntektØre = totalInntektØre,
        grunnbeløpØre = grunnbeløpØre,
        grunnbeløp6GØre = grunnbeløp6GØre,
        begrensetTil6G = totalInntektØre > grunnbeløp6GØre,
        sykepengegrunnlagØre = minOf(totalInntektØre, grunnbeløp6GØre),
        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
        opprettet = "2024-01-01T00:00:00Z",
        opprettetAv = "test",
        sistOppdatert = "2024-01-01T00:00:00Z",
    )
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

        val builder = YrkesaktivitetAssertionBuilder(yrkesaktivitetResultat!!)
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

    fun harDekningsgradBegrunnelse(begrunnelse: Beregningssporing) {
        val sporing = yrkesaktivitetResultat.dekningsgrad?.sporing
        assertEquals(begrunnelse, sporing, "Forventet $begrunnelse, men fikk $sporing")
    }

    fun `skal ha dekningsgrad begrunnelse`(begrunnelse: Beregningssporing) {
        harDekningsgradBegrunnelse(begrunnelse)
    }

    fun dag(
        dato: LocalDate,
        init: DagAssertionBuilder.() -> Unit,
    ) {
        val dag =
            yrkesaktivitetResultat.utbetalingstidslinje.find { it.dato == dato }
        assertNotNull(dag, "Fant ikke dag for dato $dato")

        val builder = DagAssertionBuilder(dag!!)
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
        val faktiskGrad = dag.økonomi.totalSykdomsgrad?.toDouble()
        assertEquals(grad.toDouble(), faktiskGrad, "Forventet totalgrad $grad, men fikk $faktiskGrad for dato ${dag.dato}")
    }

    fun `skal ha total grad`(grad: Int) {
        harTotalGrad(grad)
    }

    fun harSykdomsGrad(grad: Int) {
        val faktiskGrad = (dag.økonomi.sykdomsgrad?.dto()?.prosentDesimal ?: 0.0) * 100
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
            fail<Nothing>("Oppdrag $index finnes ikke. Total antall oppdrag: ${oppdrag.size}")
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

    fun oppdragMedMottaker(
        mottaker: String,
        init: OppdragMatcherBuilder.() -> Unit,
    ) {
        val oppdragMedMottaker =
            oppdrag.find { it.mottaker == mottaker }
        assertNotNull(oppdragMedMottaker, "Fant ikke oppdrag med mottaker $mottaker")

        val builder = OppdragMatcherBuilder(oppdragMedMottaker!!)
        builder.init()
    }

    fun `oppdrag med mottaker`(
        mottaker: String,
        init: OppdragMatcherBuilder.() -> Unit,
    ) {
        oppdragMedMottaker(mottaker, init)
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
