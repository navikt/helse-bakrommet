package no.nav.helse.bakrommet.behandling.sykepengegrunnlag

import no.nav.helse.Grunnbeløp
import no.nav.helse.bakrommet.BeregningskoderKombinasjonerSykepengegrunnlag
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering.Arbeidstaker
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering.Frilanser
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering.SelvstendigNæringsdrivende
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.repository.YrkesaktivitetRepository
import no.nav.helse.bakrommet.singleOrNone
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate

class SykepengegrunnlagBeregningHjelper(
    private val behandlingDao: BehandlingDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetRepository: YrkesaktivitetRepository,
) {
    fun beregnOgLagreSykepengegrunnlag(
        referanse: BehandlingReferanse,
        saksbehandler: Bruker,
    ): SykepengegrunnlagDbRecord? {
        // Hent nødvendige data for beregningen
        val periode =
            behandlingDao.finnBehandling(referanse.behandlingId)
                ?: throw RuntimeException("Fant ikke saksbehandlingsperiode for id ${referanse.behandlingId}")

        // TODO valider at sb på saken? Eller anta at det skjer senere

        // Hent sykepengegrunnlag
        val eksisterendeSykepengegrunnlag =
            periode.sykepengegrunnlagId?.let { sykepengegrunnlagDao.finnSykepengegrunnlag(it) }?.also {
                if (it.opprettetForBehandling != periode.id) {
                    return it
                }
            }

        val yrkesaktiviteter = yrkesaktivitetRepository.finn(BehandlingId(periode.id))

        val sykepengegrunnlag =
            beregnSykepengegrunnlag(
                yrkesaktiviteter,
                periode.skjæringstidspunkt,
            )

        return if (eksisterendeSykepengegrunnlag != null) {
            sykepengegrunnlagDao.oppdaterSykepengegrunnlag(eksisterendeSykepengegrunnlag.id, sykepengegrunnlag)
        } else {
            if (sykepengegrunnlag != null) {
                // Lagre nytt sykepengegrunnlag
                val lagret = sykepengegrunnlagDao.lagreSykepengegrunnlag(sykepengegrunnlag, saksbehandler, periode.id)
                // Knytt til saksbehandlingsperiode
                behandlingDao.oppdaterSykepengegrunnlagId(periode.id, lagret.id)
                lagret
            } else {
                null
            }
        }
    }
}

fun beregnSykepengegrunnlag(
    yrkesaktiviteter: List<Yrkesaktivitetsperiode>,
    skjæringstidspunkt: LocalDate,
): Sykepengegrunnlag {
    val grunnbeløp = Grunnbeløp.`1G`.beløp(skjæringstidspunkt)
    val grunnbeløp6G = Grunnbeløp.`6G`.beløp(skjæringstidspunkt)
    val grunnbeløpVirkningstidspunkt = Grunnbeløp.virkningstidspunktFor(grunnbeløp)

    val næringsdrivende = yrkesaktiviteter.singleOrNone { it.kategorisering is SelvstendigNæringsdrivende }

    val kombinert = næringsdrivende != null && yrkesaktiviteter.size > 1
    val næringsdel =
        if (!kombinert) {
            null
        } else {
            val pensjonsgivendeÅrsinntekt = næringsdrivende.inntektData?.omregnetÅrsinntekt ?: Inntekt.INGEN
            val pensjonsgivendeÅrsinntekt6GBegrenset = minOf(pensjonsgivendeÅrsinntekt, grunnbeløp6G)
            val pensjonsgivendeÅrsinntektBegrensetTil6G = pensjonsgivendeÅrsinntekt > grunnbeløp6G
            val andreYrkesaktiviteter =
                yrkesaktiviteter
                    .filter { it.id != næringsdrivende.id }
                    .map { it.inntektData?.omregnetÅrsinntekt }
                    .map {
                        it ?: Inntekt.INGEN
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

    val beregningsgrunnlag =
        when {
            kombinert -> {
                yrkesaktiviteter
                    .filter { it.kategorisering !is SelvstendigNæringsdrivende }
                    .map { it.inntektData?.omregnetÅrsinntekt ?: Inntekt.INGEN }
                    .summer()
                    .plus(næringsdel!!.næringsdel.tilInntekt())
            }

            else -> {
                yrkesaktiviteter
                    .map { it.inntektData?.omregnetÅrsinntekt ?: Inntekt.INGEN }
                    .summer()
            }
        }

    val sykepengegrunnlag = minOf(beregningsgrunnlag, grunnbeløp6G)
    val begrensetTil6G = beregningsgrunnlag > grunnbeløp6G

    return Sykepengegrunnlag(
        næringsdel = næringsdel,
        grunnbeløp = grunnbeløp.dto().årlig,
        beregningsgrunnlag = beregningsgrunnlag.dto().årlig,
        sykepengegrunnlag = sykepengegrunnlag.dto().årlig,
        seksG = grunnbeløp6G.dto().årlig,
        begrensetTil6G = begrensetTil6G,
        grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
        kombinertBeregningskode = yrkesaktiviteter.hentKombinertBeregningskode(),
    )
}

private fun List<Yrkesaktivitetsperiode>.hentKombinertBeregningskode(): BeregningskoderKombinasjonerSykepengegrunnlag? {
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
