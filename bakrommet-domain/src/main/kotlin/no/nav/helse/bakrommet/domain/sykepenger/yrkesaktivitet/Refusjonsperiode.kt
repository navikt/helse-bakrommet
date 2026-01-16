package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

data class Refusjonsperiode(
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: Inntekt,
)
