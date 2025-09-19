package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import java.util.UUID

enum class Beregningssporing {
    ARBEIDSTAKER_100,
    ORDINAER_SELVSTENDIG_80,
    ORDINAER_SELVSTENDIG_NAVFORSIKRING_100,
    SELVSTENDIG_KOLLEKTIVFORSIKRING_100,
    FRILANSER_100,
    INAKTIV_65,
    INAKTIV_100,
    DAGPENGEMOTTAKER_100,
}

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

data class Sporbar<T>(
    val verdi: T,
    val sporing: Beregningssporing,
)

data class YrkesaktivitetUtbetalingsberegning(
    val yrkesaktivitetId: UUID,
    val dager: List<DagUtbetalingsberegning>,
    val dekningsgrad: Sporbar<Prosentdel>?,
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
