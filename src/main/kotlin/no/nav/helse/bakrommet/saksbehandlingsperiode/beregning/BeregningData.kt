package no.nav.helse.bakrommet.saksbehandlingsperiode.beregning

import java.time.LocalDate
import java.util.UUID

data class BeregningInput(
    val dagoversikt: List<DagoversiktDag>,
    val sykepengegrunnlag: SykepengegrunnlagInput,
    val refusjon: List<RefusjonInput>,
    val maksdao: Int,
)

data class DagoversiktDag(
    val dato: LocalDate,
    val dagtype: String,
    val grad: Int?,
    val yrkesaktivitetId: UUID,
)

data class SykepengegrunnlagInput(
    val sykepengegrunnlagØre: Long,
)

data class RefusjonInput(
    val yrkesaktivitetId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val beløpØre: Long,
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
