package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.Grunnbeløp
import no.nav.helse.bakrommet.BeregningskoderKombinasjonerSykepengegrunnlag
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering.Arbeidstaker
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering.Frilanser
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering.SelvstendigNæringsdrivende
import no.nav.helse.bakrommet.util.singleOrNone
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate

class SykepengegrunnlagBeregningHjelper(
    private val beregningDao: UtbetalingsberegningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetDao: YrkesaktivitetDao,
) {
    fun beregnOgLagreSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): SykepengegrunnlagDbRecord? {
        // Hent nødvendige data for beregningen
        val periode =
            saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(referanse.periodeUUID)
                ?: throw RuntimeException("Fant ikke saksbehandlingsperiode for id ${referanse.periodeUUID}")

        // TODO valider at sb på saken? Eller anta at det skjer senere

        // Hent sykepengegrunnlag
        val eksisterendeSykepengegrunnlag =
            periode.sykepengegrunnlagId?.let { sykepengegrunnlagDao.finnSykepengegrunnlag(it) }

        val yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktiviteter(periode)

        val sykepengegrunnlag =
            beregnSykepengegrunnlag(
                yrkesaktiviteter,
                periode.skjæringstidspunkt
                    ?: throw RuntimeException("Mangler skjæringstidspunkt på periode ${periode.id}"),
            )

        return if (eksisterendeSykepengegrunnlag != null) {
            sykepengegrunnlagDao.oppdaterSykepengegrunnlag(eksisterendeSykepengegrunnlag.id, sykepengegrunnlag)
        } else {
            if (sykepengegrunnlag != null) {
                // Lagre nytt sykepengegrunnlag
                val lagret = sykepengegrunnlagDao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, periode.id)
                // Knytt til saksbehandlingsperiode
                saksbehandlingsperiodeDao.oppdaterSykepengegrunnlagId(periode.id, lagret.id)
                lagret
            } else {
                null
            }
        }
    }
}

fun beregnSykepengegrunnlag(
    yrkesaktiviteter: List<Yrkesaktivitet>,
    skjæringstidspunkt: LocalDate,
): Sykepengegrunnlag? {
    val grunnbeløp = Grunnbeløp.`1G`.beløp(skjæringstidspunkt)
    val grunnbeløp6G = Grunnbeløp.`6G`.beløp(skjæringstidspunkt)
    val grunnbeløpVirkningstidspunkt = Grunnbeløp.virkningstidspunktFor(grunnbeløp)

    val næringsdrivende = yrkesaktiviteter.singleOrNone { it.kategorisering is SelvstendigNæringsdrivende }

    val kombinert = næringsdrivende != null && yrkesaktiviteter.size > 1
    val næringsdel =
        if (!kombinert) {
            null
        } else {
            val pensjonsgivendeÅrsinntekt = Inntekt.gjenopprett(næringsdrivende.inntektData!!.omregnetÅrsinntekt)
            val pensjonsgivendeÅrsinntekt6GBegrenset = minOf(pensjonsgivendeÅrsinntekt, grunnbeløp6G)
            val pensjonsgivendeÅrsinntektBegrensetTil6G = pensjonsgivendeÅrsinntekt > grunnbeløp6G
            val andreYrkesaktiviteter =
                yrkesaktiviteter
                    .filter { it.id != næringsdrivende.id }
                    .map { it.inntektData?.omregnetÅrsinntekt }
                    .map {
                        it?.let {
                            Inntekt.gjenopprett(it)
                        } ?: Inntekt.INGEN
                    }
            val sumAvArbeidsinntekt = andreYrkesaktiviteter.fold(Inntekt.INGEN) { acc, inntekt -> acc + inntekt }

            // Trekk arbeidsinntekt fra næringsdel
            // Hvis negativt, settes til 0
            val næringsdel = maxOf(pensjonsgivendeÅrsinntekt6GBegrenset - sumAvArbeidsinntekt, Inntekt.INGEN)
            Næringsdel(
                pensjonsgivendeÅrsinntekt = pensjonsgivendeÅrsinntekt.dto().årlig,
                pensjonsgivendeÅrsinntekt6GBegrenset = pensjonsgivendeÅrsinntekt6GBegrenset.dto().årlig,
                pensjonsgivendeÅrsinntektBegrensetTil6G = pensjonsgivendeÅrsinntektBegrensetTil6G,
                næringsdel = næringsdel.dto().årlig,
                sumAvArbeidsinntekt = sumAvArbeidsinntekt.dto().årlig,
            )
        }

    val totaltInntektsgrunnlag =
        when {
            kombinert ->
                yrkesaktiviteter
                    .filter { it.kategorisering !is SelvstendigNæringsdrivende }
                    .map { it.inntektData?.omregnetÅrsinntekt?.tilInntekt() ?: Inntekt.INGEN }
                    .summer()
                    .plus(næringsdel!!.næringsdel.tilInntekt())

            else ->
                yrkesaktiviteter
                    .map { it.inntektData?.omregnetÅrsinntekt?.tilInntekt() ?: Inntekt.INGEN }
                    .summer()
        }

    val sykepengegrunnlag = minOf(totaltInntektsgrunnlag, grunnbeløp6G)
    val begrensetTil6G = totaltInntektsgrunnlag > grunnbeløp6G

    return Sykepengegrunnlag(
        næringsdel = næringsdel,
        grunnbeløp = grunnbeløp.dto().årlig,
        totaltInntektsgrunnlag = totaltInntektsgrunnlag.dto().årlig,
        sykepengegrunnlag = sykepengegrunnlag.dto().årlig,
        seksG = grunnbeløp6G.dto().årlig,
        begrensetTil6G = begrensetTil6G,
        grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
        kombinertBeregningskode = yrkesaktiviteter.hentKombinertBeregningskode(),
    )
}

private fun List<Yrkesaktivitet>.hentKombinertBeregningskode(): BeregningskoderKombinasjonerSykepengegrunnlag? {
    val erNæringsdrivende = this.any { it.kategorisering is SelvstendigNæringsdrivende }
    val erFrilanser = this.any { it.kategorisering is Frilanser }
    val erArbeidstaker = this.any { it.kategorisering is Arbeidstaker }

    if (erNæringsdrivende && erFrilanser && erArbeidstaker) {
        return BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG
    }
    if (erNæringsdrivende && erFrilanser) {
        return BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_SELVSTENDIG_FRILANSER_SYKEPENGEGRUNNLAG
    }
    if (erArbeidstaker && erFrilanser) {
        return BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SYKEPENGEGRUNNLAG
    }
    if (erNæringsdrivende && erArbeidstaker) {
        return BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_SELVSTENDIG_SYKEPENGEGRUNNLAG
    }

    return null
}
