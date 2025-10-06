package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Refusjonsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.dto.PeriodeDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Kotlin DSL for å lage testdata for utbetalingsberegning
 */
class UtbetalingsberegningTestBuilder {
    private var saksbehandlingsperiode: Saksbehandlingsperiode? = null
    private var arbeidsgiverperiode: Saksbehandlingsperiode? = null
    private val yrkesaktiviteter = mutableListOf<YrkesaktivitetBuilder>()
    private val inntekter = mutableListOf<InntektBuilder>()

    fun periode(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        saksbehandlingsperiode = Saksbehandlingsperiode(fom = fom, tom = tom)
    }

    fun arbeidsgiverperiode(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        arbeidsgiverperiode = Saksbehandlingsperiode(fom = fom, tom = tom)
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
        val yrkesaktivitetListe = yrkesaktiviteter.map { it.build() }
        val sykepengegrunnlag = lagSykepengegrunnlag(inntekter.map { it.build() })

        return UtbetalingsberegningInput(
            sykepengegrunnlag = sykepengegrunnlag,
            yrkesaktivitet = yrkesaktivitetListe,
            saksbehandlingsperiode = periode,
            arbeidsgiverperiode = arbeidsgiverperiode,
        )
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

    fun arbeidstaker() {
        inntektskategori = "ARBEIDSTAKER"
    }

    fun inaktiv(variant: String = "INAKTIV_VARIANT_A") {
        inntektskategori = "INAKTIV"
        kategorisering["VARIANT_AV_INAKTIV"] = variant
    }

    fun næringsdrivende(forsikringstype: String = "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG") {
        inntektskategori = "SELVSTENDIG_NÆRINGSDRIVENDE"
        kategorisering["SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING"] = forsikringstype
    }

    fun fra(dato: LocalDate) {
        gjeldendeDato = dato
    }

    fun arbeidsgiverperiode(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        arbeidsgiverperiode = Pair(fom, tom)
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

    fun build(): Yrkesaktivitet {
        val perioder =
            arbeidsgiverperiode?.let { (fom, tom) ->
                Perioder(
                    type = Periodetype.ARBEIDSGIVERPERIODE,
                    perioder = listOf(PeriodeDto(fom = fom, tom = tom)),
                )
            }

        kategorisering["INNTEKTSKATEGORI"] = inntektskategori

        return Yrkesaktivitet(
            id = id,
            kategorisering = kategorisering,
            kategoriseringGenerert = null,
            dagoversikt = dagoversikt,
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

    fun beløpØre(ørePerMåned: Long) {
        this.beløpPerMånedØre = ørePerMåned
    }

    fun kilde(kilde: Inntektskilde) {
        this.kilde = kilde
    }

    fun refusjon(init: RefusjonBuilder.() -> Unit) {
        val builder = RefusjonBuilder()
        builder.init()
        refusjon.add(builder.build())
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

    fun til(dato: LocalDate) {
        this.tom = dato
    }

    fun åpen() {
        this.tom = null
    }

    fun beløp(krPerMåned: Int) {
        this.beløpØre = krPerMåned * 100L
    }

    fun beløpØre(ørePerMåned: Long) {
        this.beløpØre = ørePerMåned
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
fun utbetalingsberegningTest(init: UtbetalingsberegningTestBuilder.() -> Unit): UtbetalingsberegningInput {
    val builder = UtbetalingsberegningTestBuilder()
    builder.init()
    return builder.build()
}

// Hjelpefunksjoner for å lage sykepengegrunnlag
private fun lagSykepengegrunnlag(inntekter: List<Inntekt>): SykepengegrunnlagResponse {
    val totalInntektØre = 12 * inntekter.sumOf { it.beløpPerMånedØre }
    val grunnbeløpØre = 12402800L
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
