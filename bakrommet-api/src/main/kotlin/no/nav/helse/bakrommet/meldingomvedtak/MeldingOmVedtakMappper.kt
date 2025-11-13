package no.nav.helse.bakrommet.meldingomvedtak

import no.nav.helse.bakrommet.kafka.dto.meldingomvedktak.SpilleromMeldingOmVedtakDto
import no.nav.helse.bakrommet.kafka.dto.meldingomvedktak.UtbetalingDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeKafkaDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeStatusKafkaDto

fun SaksbehandlingsperiodeKafkaDto.skalHaMeldingOmVedtak(): Boolean = this.status == SaksbehandlingsperiodeStatusKafkaDto.GODKJENT

fun SaksbehandlingsperiodeKafkaDto.tilMeldingOmVedtak(): SpilleromMeldingOmVedtakDto =
    SpilleromMeldingOmVedtakDto(
        fnr = this.fnr,
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        ubetalinger =
            this.spilleromOppdrag?.oppdrag?.map {
                UtbetalingDto(
                    beløp = it.totalbeløp,
                    mottaker = it.mottaker,
                )
            } ?: emptyList(),
    )
