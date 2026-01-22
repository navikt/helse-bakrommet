package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import java.time.LocalDate

data class SykefraværstilfelleId(
    val naturligIdent: NaturligIdent,
    val skjæringstidspunkt: LocalDate,
)
