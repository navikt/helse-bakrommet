package no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.UUID

data class Sammenlikningsgrunnlag(
    val naturligIdent: NaturligIdent,
    val skjæringstidspunkt: LocalDate,
    val totaltSammenlikningsgrunnlag: Inntekt,
    val avvikProsent: Double,
    val avvikMotInntektsgrunnlag: Inntekt,
    val basertPåDokumentId: UUID,
)
