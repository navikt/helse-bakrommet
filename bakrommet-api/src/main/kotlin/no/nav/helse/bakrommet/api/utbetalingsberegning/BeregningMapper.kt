package no.nav.helse.bakrommet.api.utbetalingsberegning

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.BeregningDataDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.BeregningResponseDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.BeregningskoderDekningsgradDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.OppdragDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.ProsentdelDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.SpilleromOppdragDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.SporbarDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.UtbetalingsdagDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.UtbetalingslinjeDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.UtbetalingstidslinjeDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.YrkesaktivitetUtbetalingsberegningDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.ØkonomiDto
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.BeregningData
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.BeregningResponse
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.Sporbar
import no.nav.helse.bakrommet.kafka.dto.oppdrag.OppdragDto as KafkaOppdragDto
import no.nav.helse.bakrommet.kafka.dto.oppdrag.SpilleromOppdragDto as KafkaSpilleromOppdragDto
import no.nav.helse.bakrommet.kafka.dto.oppdrag.UtbetalingslinjeDto as KafkaUtbetalingslinjeDto
import no.nav.helse.dto.ProsentdelDto as SpleisProsentdelDto
import no.nav.helse.dto.serialisering.UtbetalingsdagUtDto as SpleisUtbetalingsdagUtDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto as SpleisUtbetalingstidslinjeUtDto
import no.nav.helse.dto.serialisering.ØkonomiUtDto as SpleisØkonomiUtDto

fun BeregningResponse.tilBeregningResponseDto(): BeregningResponseDto =
    BeregningResponseDto(
        id = id,
        saksbehandlingsperiodeId = saksbehandlingsperiodeId,
        beregningData = beregningData.tilBeregningDataDto(),
        opprettet = opprettet,
        opprettetAv = opprettetAv,
        sistOppdatert = sistOppdatert,
    )

private fun BeregningData.tilBeregningDataDto(): BeregningDataDto =
    BeregningDataDto(
        yrkesaktiviteter =
            yrkesaktiviteter.map {
                YrkesaktivitetUtbetalingsberegningDto(
                    yrkesaktivitetId = it.yrkesaktivitetId,
                    utbetalingstidslinje = it.utbetalingstidslinje.dto().tilUtbetalingstidslinjeDto(),
                    dekningsgrad = it.dekningsgrad?.tilSporbarDto(),
                )
            },
        spilleromOppdrag = spilleromOppdrag.tilSpilleromOppdragDto(),
    )

private fun <T> Sporbar<T>.tilSporbarDto(): SporbarDto<ProsentdelDto>? {
    val prosentdelDto = verdi as? SpleisProsentdelDto ?: return null
    return SporbarDto(
        verdi = ProsentdelDto(prosentdelDto.prosentDesimal),
        sporing = sporing.tilBeregningskoderDekningsgradDto(),
    )
}

private fun BeregningskoderDekningsgrad.tilBeregningskoderDekningsgradDto(): BeregningskoderDekningsgradDto =
    when (this) {
        BeregningskoderDekningsgrad.ARBEIDSTAKER_DEKNINGSGRAD_100 -> BeregningskoderDekningsgradDto.ARBEIDSTAKER_DEKNINGSGRAD_100
        BeregningskoderDekningsgrad.SELVSTENDIG_DEKNINGSGRAD_80 -> BeregningskoderDekningsgradDto.SELVSTENDIG_DEKNINGSGRAD_80
        BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_80 -> BeregningskoderDekningsgradDto.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_80
        BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100 -> BeregningskoderDekningsgradDto.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100
        BeregningskoderDekningsgrad.SELVSTENDIG_KOLLEKTIVFORSIKRING_DEKNINGSGRAD_100 -> BeregningskoderDekningsgradDto.SELVSTENDIG_KOLLEKTIVFORSIKRING_DEKNINGSGRAD_100
        BeregningskoderDekningsgrad.FRILANSER_DEKNINGSGRAD_100 -> BeregningskoderDekningsgradDto.FRILANSER_DEKNINGSGRAD_100
        BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_65 -> BeregningskoderDekningsgradDto.INAKTIV_DEKNINGSGRAD_65
        BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_100 -> BeregningskoderDekningsgradDto.INAKTIV_DEKNINGSGRAD_100
        BeregningskoderDekningsgrad.ARBEIDSLEDIG_DEKNINGSGRAD_100 -> BeregningskoderDekningsgradDto.ARBEIDSLEDIG_DEKNINGSGRAD_100
    }

private fun SpleisUtbetalingstidslinjeUtDto.tilUtbetalingstidslinjeDto(): UtbetalingstidslinjeDto =
    UtbetalingstidslinjeDto(
        dager = dager.map { it.tilUtbetalingsdagDto() },
    )

private fun SpleisUtbetalingsdagUtDto.tilUtbetalingsdagDto(): UtbetalingsdagDto =
    UtbetalingsdagDto(
        dato = dato,
        økonomi = økonomi.tilØkonomiDto(),
    )

private fun SpleisØkonomiUtDto.tilØkonomiDto(): ØkonomiDto =
    ØkonomiDto(
        grad = grad.prosentDesimal,
        totalGrad = totalGrad.prosentDesimal,
        utbetalingsgrad = utbetalingsgrad.prosentDesimal,
        arbeidsgiverbeløp = arbeidsgiverbeløp?.dagligDouble?.beløp,
        personbeløp = personbeløp?.dagligDouble?.beløp,
    )

private fun KafkaSpilleromOppdragDto.tilSpilleromOppdragDto(): SpilleromOppdragDto =
    SpilleromOppdragDto(
        spilleromUtbetalingId = spilleromUtbetalingId,
        fnr = fnr,
        oppdrag = oppdrag.map { it.tilOppdragDto() },
        maksdato = maksdato,
    )

private fun KafkaOppdragDto.tilOppdragDto(): OppdragDto =
    OppdragDto(
        mottaker = mottaker,
        fagområde = fagområde,
        linjer = linjer.map { it.tilUtbetalingslinjeDto() },
        totalbeløp = totalbeløp,
    )

private fun KafkaUtbetalingslinjeDto.tilUtbetalingslinjeDto(): UtbetalingslinjeDto =
    UtbetalingslinjeDto(
        fom = fom,
        tom = tom,
        beløp = beløp,
        grad = grad,
        klassekode = klassekode,
        stønadsdager = stønadsdager,
    )
