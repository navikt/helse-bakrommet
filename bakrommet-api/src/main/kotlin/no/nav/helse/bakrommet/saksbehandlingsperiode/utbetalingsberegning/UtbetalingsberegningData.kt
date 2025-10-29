package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.dto.deserialisering.OppdragInnDto
import no.nav.helse.dto.deserialisering.UtbetalingstidslinjeInnDto
import no.nav.helse.dto.serialisering.OppdragUtDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.util.UUID

data class UtbetalingsberegningInput(
    val sykepengegrunnlag: Sykepengegrunnlag,
    val yrkesaktivitet: List<Yrkesaktivitet>,
    val saksbehandlingsperiode: PeriodeDto,
    val arbeidsgiverperiode: PeriodeDto? = null,
)

data class DemoUtbetalingsberegningInput(
    val sykepengegrunnlag: Sykepengegrunnlag,
    val yrkesaktivitet: List<YrkesaktivitetDbRecord>,
    val saksbehandlingsperiode: PeriodeDto,
    val arbeidsgiverperiode: PeriodeDto? = null,
)

data class Sporbar<T>(
    val verdi: T,
    val sporing: BeregningskoderDekningsgrad,
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
    val oppdrag: List<Oppdrag> = emptyList(),
)

data class BeregningDataUtDto(
    val yrkesaktiviteter: List<YrkesaktivitetUtbetalingsberegningUtDto>,
    val oppdrag: List<OppdragUtDto> = emptyList(),
)

data class BeregningDataInnDto(
    val yrkesaktiviteter: List<YrkesaktivitetUtbetalingsberegningInnDto>,
    val oppdrag: List<OppdragInnDto> = emptyList(),
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
