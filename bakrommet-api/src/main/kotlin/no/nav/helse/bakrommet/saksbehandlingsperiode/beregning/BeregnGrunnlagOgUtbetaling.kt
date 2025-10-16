@file:Suppress("ktlint:standard:filename")

package no.nav.helse.bakrommet.saksbehandlingsperiode.beregning

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagBeregningHjelper
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsBeregningHjelper
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao

interface Beregningsdaoer {
    val beregningDao: UtbetalingsberegningDao
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val personDao: PersonDao
}

fun Beregningsdaoer.beregnSykepengegrunnlagOgUtbetaling(
    ref: SaksbehandlingsperiodeReferanse,
    saksbehandler: Bruker,
) {
    SykepengegrunnlagBeregningHjelper(
        beregningDao = beregningDao,
        saksbehandlingsperiodeDao = saksbehandlingsperiodeDao,
        sykepengegrunnlagDao = sykepengegrunnlagDao,
        yrkesaktivitetDao = yrkesaktivitetDao,
    ).beregnOgLagreSykepengegrunnlag(
        referanse = ref,
        saksbehandler = saksbehandler,
    )

    beregnUtbetaling(ref, saksbehandler)
}

fun Beregningsdaoer.beregnUtbetaling(
    ref: SaksbehandlingsperiodeReferanse,
    saksbehandler: Bruker,
) {
    beregningDao.slettBeregning(ref.periodeUUID)

    UtbetalingsBeregningHjelper(
        beregningDao = beregningDao,
        saksbehandlingsperiodeDao = saksbehandlingsperiodeDao,
        sykepengegrunnlagDao = sykepengegrunnlagDao,
        yrkesaktivitetDao = yrkesaktivitetDao,
        personDao = personDao,
    ).settBeregning(
        referanse = ref,
        saksbehandler = saksbehandler,
    )
}
