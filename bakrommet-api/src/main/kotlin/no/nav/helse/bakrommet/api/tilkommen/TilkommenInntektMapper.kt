package no.nav.helse.bakrommet.api.tilkommen

import no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.api.dto.tilkommen.OpprettTilkommenInntektRequestDto
import no.nav.helse.bakrommet.api.dto.tilkommen.TilkommenInntektResponseDto
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntekt
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDbRecord
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektYrkesaktivitetType as DomainTilkommenInntektYrkesaktivitetType

fun TilkommenInntektDbRecord.tilTilkommenInntektResponseDto(): TilkommenInntektResponseDto =
    TilkommenInntektResponseDto(
        id = this.id,
        ident = this.tilkommenInntekt.ident,
        yrkesaktivitetType = this.tilkommenInntekt.yrkesaktivitetType.tilTilkommenInntektYrkesaktivitetTypeDto(),
        fom = this.tilkommenInntekt.fom,
        tom = this.tilkommenInntekt.tom,
        inntektForPerioden = this.tilkommenInntekt.inntektForPerioden,
        notatTilBeslutter = this.tilkommenInntekt.notatTilBeslutter,
        ekskluderteDager = this.tilkommenInntekt.ekskluderteDager,
        opprettet = this.opprettet,
        opprettetAvNavIdent = this.opprettetAvNavIdent,
    )

fun OpprettTilkommenInntektRequestDto.tilTilkommenInntekt(): TilkommenInntekt =
    TilkommenInntekt(
        ident = this.ident,
        yrkesaktivitetType = this.yrkesaktivitetType.tilDomainTilkommenInntektYrkesaktivitetType(),
        fom = this.fom,
        tom = this.tom,
        inntektForPerioden = this.inntektForPerioden,
        notatTilBeslutter = this.notatTilBeslutter,
        ekskluderteDager = this.ekskluderteDager,
    )

private fun DomainTilkommenInntektYrkesaktivitetType.tilTilkommenInntektYrkesaktivitetTypeDto(): TilkommenInntektYrkesaktivitetType =
    when (this) {
        DomainTilkommenInntektYrkesaktivitetType.VIRKSOMHET -> TilkommenInntektYrkesaktivitetType.VIRKSOMHET
        DomainTilkommenInntektYrkesaktivitetType.PRIVATPERSON -> TilkommenInntektYrkesaktivitetType.PRIVATPERSON
        DomainTilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE -> TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE
    }

private fun TilkommenInntektYrkesaktivitetType.tilDomainTilkommenInntektYrkesaktivitetType(): DomainTilkommenInntektYrkesaktivitetType =
    when (this) {
        TilkommenInntektYrkesaktivitetType.VIRKSOMHET -> DomainTilkommenInntektYrkesaktivitetType.VIRKSOMHET
        TilkommenInntektYrkesaktivitetType.PRIVATPERSON -> DomainTilkommenInntektYrkesaktivitetType.PRIVATPERSON
        TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE -> DomainTilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE
    }
