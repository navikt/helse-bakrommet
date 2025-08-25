package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import java.time.LocalDate
import java.util.UUID

data class UtbetalingsberegningInput(
    val sykepengegrunnlag: SykepengegrunnlagResponse,
    val inntektsforhold: List<Inntektsforhold>,
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
)

data class BeregningResponse(
    val id: UUID,
    val saksbehandlingsperiodeId: UUID,
    val beregningData: UtbetalingsberegningData,
    val opprettet: String,
    val opprettetAv: String,
    val sistOppdatert: String,
)
