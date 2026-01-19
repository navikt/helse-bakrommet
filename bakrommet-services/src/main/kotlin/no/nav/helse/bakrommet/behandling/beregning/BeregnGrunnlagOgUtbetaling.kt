@file:Suppress("ktlint:standard:filename")

package no.nav.helse.bakrommet.behandling.beregning

import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagBeregningHjelper
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDbRecord
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsBeregningHjelper
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.person.PersonPseudoIdDao
import no.nav.helse.bakrommet.repository.BehandlingRepository
import no.nav.helse.bakrommet.repository.VilkårsvurderingRepository

interface Beregningsdaoer {
    val beregningDao: UtbetalingsberegningDao
    val behandlingDao: BehandlingDao
    val behandlingRepository: BehandlingRepository
    val vilkårsvurderingRepository: VilkårsvurderingRepository
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val personPseudoIdDao: PersonPseudoIdDao
    val tilkommenInntektDao: TilkommenInntektDao
}

fun Beregningsdaoer.beregnSykepengegrunnlagOgUtbetaling(
    behandling: Behandling,
    saksbehandler: Bruker,
): SykepengegrunnlagDbRecord? =
    beregnSykepengegrunnlagOgUtbetaling(
        ref = BehandlingReferanse(behandlingId = behandling.id.value, naturligIdent = behandling.naturligIdent),
        saksbehandler = saksbehandler,
    )

fun Beregningsdaoer.beregnSykepengegrunnlagOgUtbetaling(
    ref: BehandlingReferanse,
    saksbehandler: Bruker,
): SykepengegrunnlagDbRecord? =
    SykepengegrunnlagBeregningHjelper(
        behandlingDao = behandlingDao,
        sykepengegrunnlagDao = sykepengegrunnlagDao,
        yrkesaktivitetDao = yrkesaktivitetDao,
    ).beregnOgLagreSykepengegrunnlag(
        referanse = ref,
        saksbehandler = saksbehandler,
    ).also {
        beregnUtbetaling(ref, saksbehandler)
    }

fun Beregningsdaoer.beregnUtbetaling(
    behandling: Behandling,
    saksbehandler: Bruker,
) = beregnUtbetaling(BehandlingReferanse(behandlingId = behandling.id.value, naturligIdent = behandling.naturligIdent), saksbehandler)

fun Beregningsdaoer.beregnUtbetaling(
    ref: BehandlingReferanse,
    saksbehandler: Bruker,
) {
    beregningDao.slettBeregning(ref.behandlingId, failSilently = true)

    UtbetalingsBeregningHjelper(
        beregningDao = beregningDao,
        behandlingDao = behandlingDao,
        sykepengegrunnlagDao = sykepengegrunnlagDao,
        yrkesaktivitetDao = yrkesaktivitetDao,
        vilkårsvurderingRepository = vilkårsvurderingRepository,
        tilkommenInntektDao = tilkommenInntektDao,
    ).settBeregning(
        referanse = ref,
        saksbehandler = saksbehandler,
    )
}
