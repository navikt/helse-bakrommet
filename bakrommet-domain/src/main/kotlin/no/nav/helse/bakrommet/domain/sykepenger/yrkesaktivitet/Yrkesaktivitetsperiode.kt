package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.Dag
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.Periode
import java.time.OffsetDateTime
import java.util.*

@JvmInline
value class YrkesaktivitetId(
    val value: UUID,
)

class Yrkesaktivitetsperiode(
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

    fun hentPerioderForType(periodetype: Periodetype): List<Periode> {
        val perioder = this.perioder
        return if (perioder?.type == periodetype) {
            perioder.perioder.map { Periode(it.fom, it.tom) }
        } else {
            emptyList()
        }
    }

    fun revurderUnder(behandlingId: BehandlingId): Yrkesaktivitetsperiode =
        Yrkesaktivitetsperiode(
            id = YrkesaktivitetId(UUID.randomUUID()),
            kategorisering = this.kategorisering,
            kategoriseringGenerert = kategoriseringGenerert,
            dagoversikt = this.dagoversikt,
            dagoversiktGenerert = this.dagoversiktGenerert,
            behandlingId = behandlingId,
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = generertFraDokumenter,
            perioder = perioder,
            inntektRequest = this.inntektRequest,
            inntektData = this.inntektData,
            refusjon = this.refusjon,
        )

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

    fun nyInntektRequest(request: InntektRequest) {
        val feilKategori =
            { throw IllegalStateException("Feil inntektkategori for oppdatering av inntekt med tyoe ${request.javaClass.name}") }

        when (request) {
            is InntektRequest.Arbeidstaker -> {
                if (kategorisering !is YrkesaktivitetKategorisering.Arbeidstaker) {
                    feilKategori()
                }
            }

            is InntektRequest.SelvstendigNæringsdrivende -> {
                if (kategorisering !is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende) {
                    feilKategori()
                }
            }

            is InntektRequest.Frilanser -> {
                if (kategorisering !is YrkesaktivitetKategorisering.Frilanser) {
                    feilKategori()
                }
            }

            is InntektRequest.Inaktiv -> {
                if (kategorisering !is YrkesaktivitetKategorisering.Inaktiv) {
                    feilKategori()
                }
            }

            is InntektRequest.Arbeidsledig -> {
                if (kategorisering !is YrkesaktivitetKategorisering.Arbeidsledig) {
                    feilKategori()
                }
            }
        }
        inntektRequest = request
        inntektData = null
    }

    fun leggTilArbeidsgiverperiode(perioder: List<Periode>) {
        val agpPerioder = Perioder(type = Periodetype.ARBEIDSGIVERPERIODE, perioder = perioder)
        check(kategorisering is YrkesaktivitetKategorisering.Arbeidstaker) {
            "Kan kun legge til arbeidsgiverperiode på yrkesaktiviteter av typen Arbeidstaker"
        }
        oppdaterPerioder(agpPerioder)
    }

    fun leggTilInntektData(inntektData: InntektData) {
        this.inntektData = inntektData
    }

    companion object {
        fun opprett(
            kategorisering: YrkesaktivitetKategorisering,
            kategoriseringGenerert: YrkesaktivitetKategorisering?,
            dagoversikt: Dagoversikt?,
            dagoversiktGenerert: Dagoversikt?,
            behandlingId: BehandlingId,
            generertFraDokumenter: List<UUID> = emptyList(),
        ) = Yrkesaktivitetsperiode(
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
