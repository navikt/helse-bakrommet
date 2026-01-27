package no.nav.helse.bakrommet.domain.saksbehandling.behandling

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.sykepenger.Periode
import java.time.Instant
import java.time.LocalDate
import java.util.*

@JvmInline
value class BehandlingId(
    val value: UUID,
)

class Behandling private constructor(
    val id: BehandlingId,
    val naturligIdent: NaturligIdent,
    val opprettet: Instant,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    fom: LocalDate,
    tom: LocalDate,
    status: BehandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
    beslutterNavIdent: String? = null,
    skjæringstidspunkt: LocalDate,
    individuellBegrunnelse: String? = null,
    sykepengegrunnlagId: UUID? = null,
    revurdererBehandlingId: BehandlingId? = null,
    revurdertAvBehandlingId: BehandlingId? = null,
    endringer: List<Endring>,
) {
    var fom: LocalDate = fom
        private set
    var tom: LocalDate = tom
        private set
    var status: BehandlingStatus = status
        private set
    var beslutterNavIdent: String? = beslutterNavIdent
        private set
    var skjæringstidspunkt: LocalDate = skjæringstidspunkt
        private set
    var individuellBegrunnelse: String? = individuellBegrunnelse
        private set
    var sykepengegrunnlagId: UUID? = sykepengegrunnlagId
        private set
    var revurdererBehandlingId: BehandlingId? = revurdererBehandlingId
        private set
    var revurdertAvBehandlingId: BehandlingId? = revurdertAvBehandlingId
        private set

    private val _endringer = endringer.toMutableList()
    val endringer: List<Endring> get() = _endringer

    val periode get() = Periode(fom, tom)

    fun erÅpenForEndringer() = status == BehandlingStatus.UNDER_BEHANDLING

    fun erGodkjent() = status == BehandlingStatus.GODKJENT

    fun revurderAv(
        navIdent: String,
        navn: String,
    ): Behandling =
        Behandling(
            id = BehandlingId(UUID.randomUUID()),
            naturligIdent = naturligIdent,
            opprettet = Instant.now(),
            opprettetAvNavIdent = navIdent,
            opprettetAvNavn = navn,
            fom = fom,
            tom = tom,
            status = BehandlingStatus.UNDER_BEHANDLING,
            beslutterNavIdent = null,
            skjæringstidspunkt = skjæringstidspunkt,
            individuellBegrunnelse = null,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererBehandlingId = this.id,
            revurdertAvBehandlingId = null,
            endringer = emptyList(),
        )

    infix fun gjelder(naturligIdent: NaturligIdent): Boolean = this.naturligIdent == naturligIdent

    class Endring(
        val status: BehandlingStatus,
        val beslutterNavIdent: String?,
        val tidspunkt: Instant,
        val navIdent: String,
        val type: TypeEndring,
        val kommentar: String? = null,
    ) {
        enum class TypeEndring {
            Startet,
            SendtTilBeslutning,
            TattTilBeslutning,
            SendtIRetur,
            Godkjent,
            OppdatertIndividuellBegrunnelse,
            OppdatertSkjæringstidspunkt,
            OppdatertYrkesaktivitetKategorisering,
            RevurderingStartet,
        }
    }

    companion object {
        fun fraLagring(
            id: BehandlingId,
            naturligIdent: NaturligIdent,
            opprettet: Instant,
            opprettetAvNavIdent: String,
            opprettetAvNavn: String,
            fom: LocalDate,
            tom: LocalDate,
            status: BehandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
            beslutterNavIdent: String?,
            skjæringstidspunkt: LocalDate,
            individuellBegrunnelse: String?,
            sykepengegrunnlagId: UUID?,
            revurdererBehandlingId: BehandlingId?,
            revurdertAvBehandlingId: BehandlingId?,
            endringer: List<Endring>,
        ) = Behandling(
            id = id,
            naturligIdent = naturligIdent,
            opprettet = opprettet,
            opprettetAvNavIdent = opprettetAvNavIdent,
            opprettetAvNavn = opprettetAvNavn,
            fom = fom,
            tom = tom,
            status = status,
            beslutterNavIdent = beslutterNavIdent,
            skjæringstidspunkt = skjæringstidspunkt,
            individuellBegrunnelse = individuellBegrunnelse,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererBehandlingId = revurdererBehandlingId,
            revurdertAvBehandlingId = revurdertAvBehandlingId,
            endringer = endringer,
        )
    }
}

enum class BehandlingStatus {
    UNDER_BEHANDLING,
    TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    REVURDERT,
    ;

    companion object {
        val GYLDIGE_ENDRINGER: Set<Pair<BehandlingStatus, BehandlingStatus>> =
            setOf(
                UNDER_BEHANDLING to TIL_BESLUTNING,
                TIL_BESLUTNING to UNDER_BESLUTNING,
                UNDER_BESLUTNING to GODKJENT,
                UNDER_BESLUTNING to UNDER_BEHANDLING,
                UNDER_BESLUTNING to UNDER_BESLUTNING, // ved endring av beslutter
                TIL_BESLUTNING to UNDER_BEHANDLING,
                UNDER_BEHANDLING to UNDER_BESLUTNING,
                GODKJENT to REVURDERT,
            )

        fun erGyldigEndring(fraTil: Pair<BehandlingStatus, BehandlingStatus>) = GYLDIGE_ENDRINGER.contains(fraTil)
    }
}
