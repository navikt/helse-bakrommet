package no.nav.helse.bakrommet.saksbehandlingsperiode.beregning

import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import java.time.LocalDate
import java.util.UUID

data class BeregningInput(
    val sykepengegrunnlag: SykepengegrunnlagResponse,
    val inntektsforhold: List<Inntektsforhold>,
    val maksdao: Int,
)

data class BeregningData(
    val yrkesaktiviteter: List<YrkesaktivitetBeregning>,
)

data class YrkesaktivitetBeregning(
    val yrkesaktivitetId: UUID,
    val dager: List<DagBeregning>,
)

data class DagBeregning(
    val dato: LocalDate,
    val utbetalingØre: Long,
    val refusjonØre: Long,
)

data class BeregningResponse(
    val id: UUID,
    val saksbehandlingsperiodeId: UUID,
    val beregningData: BeregningData,
    val opprettet: String,
    val opprettetAv: String,
    val sistOppdatert: String,
)
