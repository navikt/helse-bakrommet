package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.Periode
import java.time.OffsetDateTime
import java.util.UUID

data class Yrkesaktivitet(
    val id: UUID,
    val kategorisering: YrkesaktivitetKategorisering,
    val kategoriseringGenerert: YrkesaktivitetKategorisering?,
    val dagoversikt: Dagoversikt?,
    val dagoversiktGenerert: Dagoversikt?,
    val behandlingId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder? = null,
    val inntektRequest: InntektRequest? = null,
    val inntektData: InntektData? = null,
    val refusjon: List<Refusjonsperiode>? = null,
) {
    fun hentPerioderForType(periodetype: Periodetype): List<Periode> =
        if (this.perioder?.type == periodetype) {
            this.perioder.perioder.map { Periode(it.fom, it.tom) }
        } else {
            emptyList()
        }
}
