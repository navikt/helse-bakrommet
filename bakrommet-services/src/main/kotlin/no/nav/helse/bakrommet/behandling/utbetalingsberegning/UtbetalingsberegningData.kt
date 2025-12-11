package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagBase
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDbRecord
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.kafka.dto.oppdrag.SpilleromOppdragDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.dto.deserialisering.UtbetalingstidslinjeInnDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.util.UUID

data class UtbetalingsberegningInput(
    val sykepengegrunnlag: SykepengegrunnlagBase,
    val yrkesaktivitet: List<Yrkesaktivitet>,
    val saksbehandlingsperiode: PeriodeDto,
    val arbeidsgiverperiode: PeriodeDto? = null,
    val tilkommenInntekt: List<TilkommenInntektDbRecord>,
    val vilkår: List<VurdertVilkår>,
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
    val spilleromOppdrag: SpilleromOppdragDto,
)

data class BeregningDataUtDto(
    val yrkesaktiviteter: List<YrkesaktivitetUtbetalingsberegningUtDto>,
    val spilleromOppdrag: SpilleromOppdragDto,
)

data class BeregningDataInnDto(
    val yrkesaktiviteter: List<YrkesaktivitetUtbetalingsberegningInnDto>,
    val spilleromOppdrag: SpilleromOppdragDto,
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
