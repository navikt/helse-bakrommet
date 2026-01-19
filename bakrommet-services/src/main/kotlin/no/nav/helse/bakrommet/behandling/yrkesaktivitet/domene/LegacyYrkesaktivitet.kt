package no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene

import no.nav.helse.bakrommet.behandling.inntekter.InntektDataOld
import no.nav.helse.bakrommet.behandling.inntekter.domain.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.hendelser.Periode
import java.time.OffsetDateTime
import java.util.UUID

data class LegacyYrkesaktivitet(
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
    val inntektData: InntektDataOld? = null,
    val refusjon: List<Refusjonsperiode>? = null,
) {
    fun hentPerioderForType(periodetype: Periodetype): List<Periode> =
        if (this.perioder?.type == periodetype) {
            this.perioder.perioder.map { Periode(it.fom, it.tom) }
        } else {
            emptyList()
        }
}
