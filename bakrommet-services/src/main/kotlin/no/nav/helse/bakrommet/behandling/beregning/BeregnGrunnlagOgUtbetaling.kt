@file:Suppress("ktlint:standard:filename")

package no.nav.helse.bakrommet.behandling.beregning

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagBeregningHjelper
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDbRecord
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsBeregningHjelper
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.person.PersonDao

interface Beregningsdaoer {
    val beregningDao: UtbetalingsberegningDao
    val behandlingDao: BehandlingDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val personDao: PersonDao
    val tilkommenInntektDao: TilkommenInntektDao
}

fun Beregningsdaoer.beregnSykepengegrunnlagOgUtbetaling(
    ref: SaksbehandlingsperiodeReferanse,
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
    ref: SaksbehandlingsperiodeReferanse,
    saksbehandler: Bruker,
) {
    beregningDao.slettBeregning(ref.periodeUUID, failSilently = true)

    UtbetalingsBeregningHjelper(
        beregningDao = beregningDao,
        behandlingDao = behandlingDao,
        sykepengegrunnlagDao = sykepengegrunnlagDao,
        yrkesaktivitetDao = yrkesaktivitetDao,
        personDao = personDao,
        tilkommenInntektDao = tilkommenInntektDao,
    ).settBeregning(
        referanse = ref,
        saksbehandler = saksbehandler,
    )
}
