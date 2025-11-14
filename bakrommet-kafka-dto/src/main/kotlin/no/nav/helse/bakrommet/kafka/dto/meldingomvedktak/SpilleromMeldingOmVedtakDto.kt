package no.nav.helse.bakrommet.kafka.dto.meldingomvedktak

import java.time.LocalDate
import java.util.UUID

data class SpilleromMeldingOmVedtakDto(
    val fnr: String,
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val ubetalinger: List<UtbetalingDto>,
    val revurdererSaksbehandlingsperiodeId: UUID?,
)

data class UtbetalingDto(
    val beløp: Int,
    val mottaker: String, // Orgnummer eller fnr
)
