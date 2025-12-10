package no.nav.helse.bakrommet.api.dto.tidslinje

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse
import java.time.LocalDate
import java.util.UUID

data class TidslinjeBehandlingDto(
    val id: UUID,
    val status: TidslinjeBehandlingStatus,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val yrkesaktiviteter: List<TidlinjeYrkesaktivitetDto>,
    val tilkommenInntekt: List<TidlinjeTilkommenInntektDto>,
    val revurdererBehandlingId: UUID?,
    val revurdertAvBehandlingId: UUID?,
) : ApiResponse

data class TidlinjeYrkesaktivitetDto(
    val id: UUID,
    val sykmeldt: Boolean,
    val orgnavn: String?,
    val orgnummer: String?,
    val yrkesaktivitetType: YrkesaktivitetType,
)

enum class TidslinjeBehandlingStatus {
    UNDER_BEHANDLING,
    TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    REVURDERT,
}

enum class YrkesaktivitetType {
    ARBEIDSTAKER,
    FRILANSER,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    INAKTIV,
    ARBEIDSLEDIG,
}

data class TidlinjeTilkommenInntektDto(
    val id: UUID,
    val ident: String,
    val orgnavn: String?,
    val yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
)

enum class TilkommenInntektYrkesaktivitetType {
    VIRKSOMHET,
    PRIVATPERSON,
    NÆRINGSDRIVENDE,
}
