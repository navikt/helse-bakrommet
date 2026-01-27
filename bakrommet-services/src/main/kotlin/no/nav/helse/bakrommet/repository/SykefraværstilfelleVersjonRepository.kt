package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.SykefraværstilfelleVersjon
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.SykefraværstilfelleVersjonId

interface SykefraværstilfelleVersjonRepository {
    fun lagre(sykefraværstilfelleVersjon: SykefraværstilfelleVersjon)

    fun finn(sykefraværstilfelleVersjonId: SykefraværstilfelleVersjonId): SykefraværstilfelleVersjon?
}
