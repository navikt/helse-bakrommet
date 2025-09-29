package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.dto.deserialisering.UtbetalingstidslinjeInnDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
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

data class Sporbar<T>(
    val verdi: T,
    val sporing: Beregningssporing,
)

data class YrkesaktivitetUtbetalingsberegning(
    val yrkesaktivitetId: UUID,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val dekningsgrad: Sporbar<ProsentdelDto>?,
)

data class YrkesaktivitetUtbetalingsberegningUtDto(
    val yrkesaktivitetId: UUID,
    val utbetalingstidslinje: UtbetalingstidslinjeUtDto,
    val dekningsgrad: Sporbar<ProsentdelDto>?,
)

data class YrkesaktivitetUtbetalingsberegningInnDto(
    val yrkesaktivitetId: UUID,
    val utbetalingstidslinje: UtbetalingstidslinjeInnDto,
    val dekningsgrad: Sporbar<ProsentdelDto>?,
)

data class BeregningData(
    val yrkesaktiviteter: List<YrkesaktivitetUtbetalingsberegning>,
)

data class BeregningDataUtDto(
    val yrkesaktiviteter: List<YrkesaktivitetUtbetalingsberegningUtDto>,
)

data class BeregningDataInnDto(
    val yrkesaktiviteter: List<YrkesaktivitetUtbetalingsberegningInnDto>,
)

data class BeregningResponse(
    val id: UUID,
    val saksbehandlingsperiodeId: UUID,
    val beregningData: BeregningData,
    val opprettet: String,
    val opprettetAv: String,
    val sistOppdatert: String,
)

data class BeregningResponseUtDto(
    val id: UUID,
    val saksbehandlingsperiodeId: UUID,
    val beregningData: BeregningDataUtDto,
    val opprettet: String,
    val opprettetAv: String,
    val sistOppdatert: String,
)
