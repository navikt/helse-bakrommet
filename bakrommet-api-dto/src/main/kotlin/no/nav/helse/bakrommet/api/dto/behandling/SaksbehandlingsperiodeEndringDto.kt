package no.nav.helse.bakrommet.api.dto.behandling

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import java.time.OffsetDateTime
import java.util.UUID

data class SaksbehandlingsperiodeEndringDto(
    val saksbehandlingsperiodeId: UUID,
    val status: TidslinjeBehandlingStatus,
    val beslutterNavIdent: String?,
    val endretTidspunkt: OffsetDateTime,
    val endretAvNavIdent: String,
    val endringType: SaksbehandlingsperiodeEndringTypeDto,
    val endringKommentar: String? = null,
) : ApiResponse

enum class SaksbehandlingsperiodeEndringTypeDto {
    STARTET,
    SENDT_TIL_BESLUTNING,
    TATT_TIL_BESLUTNING,
    SENDT_I_RETUR,
    GODKJENT,
    OPPDATERT_INDIVIDUELL_BEGRUNNELSE,
    OPPDATERT_SKJÆRINGSTIDSPUNKT,
    OPPDATERT_YRKESAKTIVITET_KATEGORISERING,
    REVURDERING_STARTET,
}
