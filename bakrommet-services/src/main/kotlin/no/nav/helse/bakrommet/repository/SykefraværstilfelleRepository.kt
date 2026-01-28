package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Sykefraværstilfelle
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.SykefraværstilfelleId

interface SykefraværstilfelleRepository {
    fun lagre(sykefraværstilfelle: Sykefraværstilfelle)

    fun finn(sykefraværstilfelleId: SykefraværstilfelleId): Sykefraværstilfelle?
}
