package no.nav.helse.bakrommet.behandling.sykepengegrunnlag

import no.nav.helse.Grunnbeløp
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.*
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.beregning.beregnUtbetaling
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import java.math.BigDecimal
import java.time.LocalDate

interface SykepengegrunnlagServiceDaoer : Beregningsdaoer

class SykepengegrunnlagService(
    private val db: DbDaoer<SykepengegrunnlagServiceDaoer>,
) {
    suspend fun hentSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse): SykepengegrunnlagResponse? =
        db.nonTransactional {
            behandlingDao
                .hentPeriode(
                    referanse,
                    krav = null,
                    måVæreUnderBehandling = false,
                ).sykepengegrunnlagId
                ?.let { sykepengegrunnlagId ->
                    sykepengegrunnlagDao.finnSykepengegrunnlag(sykepengegrunnlagId)?.let { record ->
                        SykepengegrunnlagResponse(
                            sykepengegrunnlag = record.sykepengegrunnlag,
                            sammenlikningsgrunnlag = record.sammenlikningsgrunnlag,
                            opprettetForBehandling = record.opprettetForBehandling,
                        )
                    }
                }
        }

    suspend fun opprettSykepengegrunnlag(
        request: OpprettSykepengegrunnlagRequest,
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        db.transactional {
            val behandling =
                behandlingDao.hentPeriode(
                    referanse,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            if (behandling.sykepengegrunnlagId != null) {
                throw IllegalStateException("Sykepengegrunnlag er allerede opprettet for behandling ${behandling.id}")
            }

            val spg = skapSykepengegrunnlag(behandling, request)
            val lagret = sykepengegrunnlagDao.lagreSykepengegrunnlag(spg, saksbehandler, behandling.id)
            behandlingDao.oppdaterSykepengegrunnlagId(behandling.id, lagret.id)
            beregnUtbetaling(referanse, saksbehandler)
        }
        return hentSykepengegrunnlag(referanse)!!
    }
}

fun skapSykepengegrunnlag(
    behandling: Behandling,
    request: OpprettSykepengegrunnlagRequest,
): SykepengegrunnlagBase {
    val gDato = request.datoForGBegrensning ?: behandling.skjæringstidspunkt

    val grunnbeløp = Grunnbeløp.`1G`.beløp(gDato)
    val grunnbeløp6G = Grunnbeløp.`6G`.beløp(gDato)
    val grunnbeløpVirkningstidspunkt = Grunnbeløp.virkningstidspunktFor(grunnbeløp)

    val beregningsgrunnlag = InntektbeløpDto.Årlig(request.beregningsgrunnlag.toDouble()).tilInntekt()

    val sykepengegrunnlag = minOf(beregningsgrunnlag, grunnbeløp6G)
    val begrensetTil6G = beregningsgrunnlag > grunnbeløp6G

    return FrihåndSykepengegrunnlag(
        grunnbeløp = grunnbeløp.dto().årlig,
        beregningsgrunnlag = beregningsgrunnlag.dto().årlig,
        sykepengegrunnlag = sykepengegrunnlag.dto().årlig,
        seksG = grunnbeløp6G.dto().årlig,
        begrensetTil6G = begrensetTil6G,
        grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
        begrunnelse = request.begrunnelse,
        beregningskoder = request.beregningskoder,
    )
}

data class OpprettSykepengegrunnlagRequest(
    val beregningsgrunnlag: BigDecimal,
    val begrunnelse: String,
    val datoForGBegrensning: LocalDate? = null,
    val beregningskoder: List<BeregningskoderSykepengegrunnlag>,
)
