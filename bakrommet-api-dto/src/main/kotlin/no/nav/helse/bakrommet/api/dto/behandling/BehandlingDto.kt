package no.nav.helse.bakrommet.api.dto.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BehandlingDto(
    val id: UUID,
    val spilleromPersonId: String,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: TidslinjeBehandlingStatus,
    val beslutterNavIdent: String? = null,
    val skjæringstidspunkt: LocalDate,
    val individuellBegrunnelse: String? = null,
    val sykepengegrunnlagId: UUID? = null,
    val revurdererSaksbehandlingsperiodeId: UUID? = null,
    val revurdertAvBehandlingId: UUID? = null,
) : ApiResponse
