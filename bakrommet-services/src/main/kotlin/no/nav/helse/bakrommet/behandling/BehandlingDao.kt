package no.nav.helse.bakrommet.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BehandlingDbRecord(
    val id: UUID,
    val naturligIdent: NaturligIdent,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: BehandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
    val beslutterNavIdent: String? = null,
    val skjæringstidspunkt: LocalDate,
    val individuellBegrunnelse: String? = null,
    val sykepengegrunnlagId: UUID? = null,
    val revurdererSaksbehandlingsperiodeId: UUID? = null,
    val revurdertAvBehandlingId: UUID? = null,
)

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

interface BehandlingDao {
    fun hentAlleBehandlinger(): List<BehandlingDbRecord>

    fun finnBehandling(id: UUID): BehandlingDbRecord?

    fun finnBehandlingerForNaturligIdent(naturligIdent: NaturligIdent): List<BehandlingDbRecord>

    fun finnBehandlingerForNaturligIdentSomOverlapper(
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        tom: LocalDate,
    ): List<BehandlingDbRecord>

    fun endreStatus(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
    )

    fun endreStatusOgIndividuellBegrunnelse(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
        individuellBegrunnelse: String?,
    )

    fun endreStatusOgBeslutter(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
        beslutterNavIdent: String?,
    )

    fun opprettPeriode(periode: BehandlingDbRecord)

    fun oppdaterSkjæringstidspunkt(
        behandlingId: UUID,
        skjæringstidspunkt: LocalDate,
    )

    fun oppdaterSykepengegrunnlagId(
        behandlingId: UUID,
        sykepengegrunnlagId: UUID?,
    )

    fun oppdaterRevurdertAvBehandlingId(
        behandlingId: UUID,
        revurdertAvBehandlingId: UUID,
    )
}
