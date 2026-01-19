package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.Periode
import java.time.OffsetDateTime
import java.util.UUID

@JvmInline
value class YrkesaktivitetId(
    val value: UUID,
)

class Yrkesaktivitet(
    val id: YrkesaktivitetId,
    kategorisering: YrkesaktivitetKategorisering,
    val kategoriseringGenerert: YrkesaktivitetKategorisering?,
    val dagoversikt: Dagoversikt?,
    val dagoversiktGenerert: Dagoversikt?,
    val behandlingId: BehandlingId,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder? = null,
    inntektRequest: InntektRequest? = null,
    inntektData: InntektData? = null,
    val refusjon: List<Refusjonsperiode>? = null,
) {
    var kategorisering: YrkesaktivitetKategorisering = kategorisering
        private set
    var inntektRequest: InntektRequest? = inntektRequest
        private set
    var inntektData: InntektData? = inntektData
        private set

    fun hentPerioderForType(periodetype: Periodetype): List<Periode> =
        if (this.perioder?.type == periodetype) {
            this.perioder.perioder.map { Periode(it.fom, it.tom) }
        } else {
            emptyList()
        }

    fun nyKategorisering(nyKategorisering: YrkesaktivitetKategorisering) {
        kategorisering = nyKategorisering
        inntektData = null
        inntektRequest = null
    }

    fun tilhører(behandling: Behandling): Boolean = this.behandlingId == behandling.id

    companion object {
        fun opprett(
            kategorisering: YrkesaktivitetKategorisering,
            kategoriseringGenerert: YrkesaktivitetKategorisering?,
            dagoversikt: Dagoversikt?,
            dagoversiktGenerert: Dagoversikt?,
            behandlingId: BehandlingId,
            generertFraDokumenter: List<UUID> = emptyList(),
        ) = Yrkesaktivitet(
            id = YrkesaktivitetId(UUID.randomUUID()),
            kategorisering = kategorisering,
            kategoriseringGenerert = kategoriseringGenerert,
            dagoversikt = dagoversikt,
            dagoversiktGenerert = dagoversiktGenerert,
            behandlingId = behandlingId,
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = generertFraDokumenter,
            perioder = null,
            inntektRequest = null,
            inntektData = null,
            refusjon = null,
        )
    }
}
