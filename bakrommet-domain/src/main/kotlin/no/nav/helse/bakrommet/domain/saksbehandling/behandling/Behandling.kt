package no.nav.helse.bakrommet.domain.saksbehandling.behandling

import no.nav.helse.bakrommet.domain.person.NaturligIdent
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
    var revurdererSaksbehandlingsperiodeId: BehandlingId? = revurdererBehandlingId
        private set
    var revurdertAvBehandlingId: BehandlingId? = revurdertAvBehandlingId
        private set

    fun erÅpenForEndringer() = status == BehandlingStatus.UNDER_BEHANDLING

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
            beslutterNavIdent: String? = null,
            skjæringstidspunkt: LocalDate,
            individuellBegrunnelse: String? = null,
            sykepengegrunnlagId: UUID? = null,
            revurdererSaksbehandlingsperiodeId: BehandlingId? = null,
            revurdertAvBehandlingId: BehandlingId? = null,
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
            `skjæringstidspunkt` = skjæringstidspunkt,
            individuellBegrunnelse = individuellBegrunnelse,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererBehandlingId = revurdererSaksbehandlingsperiodeId,
            revurdertAvBehandlingId = revurdertAvBehandlingId,
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
