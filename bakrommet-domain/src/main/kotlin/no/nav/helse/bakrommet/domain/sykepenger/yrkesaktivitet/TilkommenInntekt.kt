package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JvmInline
value class TilkommenInntektId(
    val value: UUID,
)

class TilkommenInntekt private constructor(
    val id: TilkommenInntektId,
    val behandlingId: BehandlingId,
    yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    fom: LocalDate,
    tom: LocalDate,
    ident: String,
    inntektForPerioden: BigDecimal,
    notatTilBeslutter: String,
    ekskluderteDager: List<LocalDate>,
    opprettet: OffsetDateTime,
    opprettetAvNavIdent: String,
) {
    val periode get() = Periode(fom, tom)

    var yrkesaktivitetType: TilkommenInntektYrkesaktivitetType = yrkesaktivitetType
        private set
    var fom: LocalDate = fom
        private set
    var tom: LocalDate = tom
        private set

    var ident: String = ident
        private set
    var inntektForPerioden: BigDecimal = inntektForPerioden
        private set
    var notatTilBeslutter: String = notatTilBeslutter
        private set
    var ekskluderteDager: List<LocalDate> = ekskluderteDager
        private set
    var opprettet: OffsetDateTime = opprettet
        private set
    var opprettetAvNavIdent: String = opprettetAvNavIdent
        private set

    fun endre(
        ident: String,
        yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
        fom: LocalDate,
        tom: LocalDate,
        inntektForPerioden: BigDecimal,
        notatTilBeslutter: String,
        ekskluderteDager: List<LocalDate>,
    ) {
        this.ident = ident
        this.yrkesaktivitetType = yrkesaktivitetType
        this.fom = fom
        this.tom = tom
        this.inntektForPerioden = inntektForPerioden
        this.notatTilBeslutter = notatTilBeslutter
        this.ekskluderteDager = ekskluderteDager
    }

    companion object {
        fun fraLagring(
            id: TilkommenInntektId,
            behandlingId: BehandlingId,
            ident: String,
            yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
            fom: LocalDate,
            tom: LocalDate,
            inntektForPerioden: BigDecimal,
            notatTilBeslutter: String,
            ekskluderteDager: List<LocalDate>,
            opprettet: OffsetDateTime,
            opprettetAvNavIdent: String,
        ) = TilkommenInntekt(
            id = id,
            behandlingId = behandlingId,
            yrkesaktivitetType = yrkesaktivitetType,
            fom = fom,
            tom = tom,
            ident = ident,
            inntektForPerioden = inntektForPerioden,
            notatTilBeslutter = notatTilBeslutter,
            ekskluderteDager = ekskluderteDager,
            opprettet = opprettet,
            opprettetAvNavIdent = opprettetAvNavIdent,
        )

        fun ny(
            behandlingId: BehandlingId,
            ident: String,
            yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
            fom: LocalDate,
            tom: LocalDate,
            inntektForPerioden: BigDecimal,
            notatTilBeslutter: String,
            ekskluderteDager: List<LocalDate>,
            opprettetAvNavIdent: String,
        ) = TilkommenInntekt(
            id = TilkommenInntektId(UUID.randomUUID()),
            behandlingId = behandlingId,
            yrkesaktivitetType = yrkesaktivitetType,
            fom = fom,
            tom = tom,
            ident = ident,
            inntektForPerioden = inntektForPerioden,
            notatTilBeslutter = notatTilBeslutter,
            ekskluderteDager = ekskluderteDager,
            opprettet = OffsetDateTime.now(),
            opprettetAvNavIdent = opprettetAvNavIdent,
        )
    }
}

enum class TilkommenInntektYrkesaktivitetType {
    VIRKSOMHET,
    PRIVATPERSON,
    NÆRINGSDRIVENDE,
}
