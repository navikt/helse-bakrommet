package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.Dag
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
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
    dagoversikt: Dagoversikt?,
    val dagoversiktGenerert: Dagoversikt?,
    val behandlingId: BehandlingId,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
    perioder: Perioder? = null,
    inntektRequest: InntektRequest? = null,
    inntektData: InntektData? = null,
    refusjon: List<Refusjonsperiode>? = null,
) {
    var kategorisering: YrkesaktivitetKategorisering = kategorisering
        private set
    var inntektRequest: InntektRequest? = inntektRequest
        private set
    var inntektData: InntektData? = inntektData
        private set
    var dagoversikt: Dagoversikt? = dagoversikt
        private set
    var perioder: Perioder? = perioder
        private set
    var refusjon: List<Refusjonsperiode>? = refusjon
        private set

    fun nyKategorisering(nyKategorisering: YrkesaktivitetKategorisering) {
        kategorisering = nyKategorisering
        inntektData = null
        inntektRequest = null
    }

    fun tilhører(behandling: Behandling): Boolean = this.behandlingId == behandling.id

    fun oppdaterDagoversikt(dager: List<Dag>) {
        val dagoversikt = requireNotNull(dagoversikt) { "Kan ikke oppdatere dager på en yrkesaktivitet uten dagoversikt" }
        this.dagoversikt = dagoversikt.nyDagoversikt(dager)
    }

    fun oppdaterPerioder(perioder: Perioder?) {
        this.perioder = perioder
    }

    fun oppdaterRefusjon(refusjon: List<Refusjonsperiode>?) {
        this.refusjon = refusjon
    }

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
