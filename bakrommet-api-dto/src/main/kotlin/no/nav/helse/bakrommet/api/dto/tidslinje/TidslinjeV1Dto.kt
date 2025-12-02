package no.nav.helse.bakrommet.api.dto.tidslinje

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse
import java.time.LocalDate
import java.util.UUID

data class TidslinjeV1Dto(
    val rader: List<TidslinjeRadV1Dto>,
) : ApiResponse

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tidslinjeRadType")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = TidslinjeRadV1Dto.OpprettetBehandlingV1Dto::class, name = "OpprettetBehandling"),
        JsonSubTypes.Type(value = TidslinjeRadV1Dto.SykmeldtYrkesaktivitetV1Dto::class, name = "SykmeldtYrkesaktivitet"),
        JsonSubTypes.Type(value = TidslinjeRadV1Dto.TilkommenInntektV1Dto::class, name = "TilkommenInntekt"),
    ],
)
sealed class TidslinjeRadV1Dto {
    abstract val tidslinjeElementer: List<TidslinjeElementV1Dto>
    abstract val id: String
    abstract val navn: String

    data class OpprettetBehandlingV1Dto(
        override val tidslinjeElementer: List<TidslinjeElementV1Dto>,
    ) : TidslinjeRadV1Dto() {
        override val id: String = "OPPRETTET_BEHANDLING"
        override val navn: String = "Opprettet behandling"
    }

    data class SykmeldtYrkesaktivitetV1Dto(
        override val tidslinjeElementer: List<YrkesaktivitetTidslinjeElementV1Dto>,
        override val id: String,
        override val navn: String,
    ) : TidslinjeRadV1Dto()

    data class TilkommenInntektV1Dto(
        override val tidslinjeElementer: List<TilkommenInntektTidslinjeElementV1Dto>,
        override val id: String,
        override val navn: String,
    ) : TidslinjeRadV1Dto()
}

open class TidslinjeElementV1Dto(
    open val fom: LocalDate,
    open val tom: LocalDate,
    open val skjæringstidspunkt: LocalDate,
    open val behandlingId: UUID,
    open val status: BehandlingStatusV1Dto,
    open val historisk: Boolean,
    open val revurdererBehandlingId: UUID?,
    open val revurdertAv: UUID?,
    open val historiske: List<TidslinjeElementV1Dto>,
)

data class YrkesaktivitetTidslinjeElementV1Dto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val skjæringstidspunkt: LocalDate,
    override val behandlingId: UUID,
    override val status: BehandlingStatusV1Dto,
    override val historisk: Boolean,
    override val revurdererBehandlingId: UUID?,
    override val revurdertAv: UUID?,
    val yrkesaktivitetId: UUID,
    val ghost: Boolean,
    override val historiske: List<YrkesaktivitetTidslinjeElementV1Dto>,
) : TidslinjeElementV1Dto(
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingId = behandlingId,
        status = status,
        historisk = historisk,
        revurdererBehandlingId = revurdererBehandlingId,
        revurdertAv = revurdertAv,
        historiske = historiske,
    )

data class TilkommenInntektTidslinjeElementV1Dto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val skjæringstidspunkt: LocalDate,
    override val behandlingId: UUID,
    override val status: BehandlingStatusV1Dto,
    override val historisk: Boolean,
    override val revurdererBehandlingId: UUID?,
    override val revurdertAv: UUID?,
    val tilkommenInntektId: UUID,
    override val historiske: List<TilkommenInntektTidslinjeElementV1Dto>,
) : TidslinjeElementV1Dto(
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingId = behandlingId,
        status = status,
        historisk = historisk,
        revurdererBehandlingId = revurdererBehandlingId,
        revurdertAv = revurdertAv,
        historiske = historiske,
    )

enum class BehandlingStatusV1Dto {
    UNDER_BEHANDLING,
    TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    REVURDERT,
}
