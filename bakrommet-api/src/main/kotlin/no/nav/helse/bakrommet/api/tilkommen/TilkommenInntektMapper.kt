package no.nav.helse.bakrommet.api.tilkommen

import no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetTypeDto
import no.nav.helse.bakrommet.api.dto.tilkommen.TilkommenInntektResponseDto
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektYrkesaktivitetType as DomainTilkommenInntektYrkesaktivitetType

fun no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt.tilTilkommenInntektResponseDto(): TilkommenInntektResponseDto =
    TilkommenInntektResponseDto(
        id = this.id.value,
        ident = this.ident,
        yrkesaktivitetType = this.yrkesaktivitetType.tilTilkommenInntektYrkesaktivitetTypeDto(),
        fom = this.fom,
        tom = this.tom,
        inntektForPerioden = this.inntektForPerioden,
        notatTilBeslutter = this.notatTilBeslutter,
        ekskluderteDager = this.ekskluderteDager,
        opprettet = this.opprettet,
        opprettetAvNavIdent = this.opprettetAvNavIdent,
    )

private fun DomainTilkommenInntektYrkesaktivitetType.tilTilkommenInntektYrkesaktivitetTypeDto(): TilkommenInntektYrkesaktivitetTypeDto =
    when (this) {
        DomainTilkommenInntektYrkesaktivitetType.VIRKSOMHET -> TilkommenInntektYrkesaktivitetTypeDto.VIRKSOMHET
        DomainTilkommenInntektYrkesaktivitetType.PRIVATPERSON -> TilkommenInntektYrkesaktivitetTypeDto.PRIVATPERSON
        DomainTilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE -> TilkommenInntektYrkesaktivitetTypeDto.NÆRINGSDRIVENDE
    }
