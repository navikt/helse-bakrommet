package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.dto.InntektbeløpDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Sykepengegrunnlag(
    val grunnbeløp: InntektbeløpDto.Årlig,
    val sykepengegrunnlag: InntektbeløpDto.Årlig,
    val seksG: InntektbeløpDto.Årlig,
    val begrensetTil6G: Boolean,
    val grunnbeløpVirkningstidspunkt: LocalDate,
    val opprettet: String,
    val opprettetAv: String,
)

data class SykepengegrunnlagDbRecord(
    val sykepengegrunnlag: Sykepengegrunnlag,
    val id: UUID,
    val opprettetAv: String,
    val opprettet: Instant,
    val oppdatert: Instant,
)
