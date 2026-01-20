package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JvmInline
value class TilkommenInntektId(
    val value: UUID,
)

class TilkommenInntekt(
    val id: TilkommenInntektId,
    val behandlingId: BehandlingId,
    val ident: NaturligIdent,
    val yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektForPerioden: BigDecimal,
    val notatTilBeslutter: String,
    val ekskluderteDager: List<LocalDate>,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
)

enum class TilkommenInntektYrkesaktivitetType {
    VIRKSOMHET,
    PRIVATPERSON,
    NÆRINGSDRIVENDE,
}
