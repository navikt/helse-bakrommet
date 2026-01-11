package no.nav.helse.bakrommet.behandling.tilkommen

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class TilkommenInntektDbRecord(
    val id: UUID,
    val behandlingId: UUID,
    val tilkommenInntekt: TilkommenInntekt,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
)

enum class TilkommenInntektYrkesaktivitetType {
    VIRKSOMHET,
    PRIVATPERSON,
    NÆRINGSDRIVENDE,
}

data class TilkommenInntekt(
    val ident: String,
    val yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektForPerioden: BigDecimal,
    val notatTilBeslutter: String,
    val ekskluderteDager: List<LocalDate>,
)

interface TilkommenInntektDao {
    fun opprett(tilkommenInntektDbRecord: TilkommenInntektDbRecord): TilkommenInntektDbRecord

    fun hentForBehandling(behandlingId: UUID): List<TilkommenInntektDbRecord>

    fun oppdater(
        id: UUID,
        tilkommenInntekt: TilkommenInntekt,
    ): TilkommenInntektDbRecord

    fun slett(
        behandlingId: UUID,
        id: UUID,
    )

    fun hent(id: UUID): TilkommenInntektDbRecord?

    fun finnTilkommenInntektForBehandlinger(map: List<UUID>): List<TilkommenInntektDbRecord>
}
