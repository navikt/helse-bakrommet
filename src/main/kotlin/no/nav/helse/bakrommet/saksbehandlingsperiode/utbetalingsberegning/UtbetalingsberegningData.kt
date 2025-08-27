package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import java.time.LocalDate
import java.util.UUID

data class UtbetalingsberegningInput(
    val sykepengegrunnlag: SykepengegrunnlagResponse,
    val yrkesaktivitet: List<Yrkesaktivitet>,
    val saksbehandlingsperiode: Saksbehandlingsperiode,
)

data class Saksbehandlingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class UtbetalingsberegningData(
    val yrkesaktiviteter: List<YrkesaktivitetUtbetalingsberegning>,
)

data class YrkesaktivitetUtbetalingsberegning(
    val yrkesaktivitetId: UUID,
    val dager: List<DagUtbetalingsberegning>,
)

data class DagUtbetalingsberegning(
    val dato: LocalDate,
    val utbetalingØre: Long,
    val refusjonØre: Long,
    val totalGrad: Int,
)

data class BeregningResponse(
    val id: UUID,
    val saksbehandlingsperiodeId: UUID,
    val beregningData: UtbetalingsberegningData,
    val opprettet: String,
    val opprettetAv: String,
    val sistOppdatert: String,
)
