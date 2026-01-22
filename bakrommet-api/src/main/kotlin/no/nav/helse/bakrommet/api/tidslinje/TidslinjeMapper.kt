package no.nav.helse.bakrommet.api.tidslinje

import no.nav.helse.bakrommet.api.dto.tidslinje.TidlinjeTilkommenInntektDto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidlinjeYrkesaktivitetDto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingDto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetTypeDto
import no.nav.helse.bakrommet.api.dto.tidslinje.YrkesaktivitetTypeDto
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.*
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.maybeOrgnummer
import no.nav.helse.bakrommet.infrastruktur.provider.Organisasjon

fun TidslinjeData.tilTidslinjeDto(): List<TidslinjeBehandlingDto> =
    behandlinger.map { behandling ->
        val behandlingYrkesaktiviteter = yrkesaktiviteter.filter { it.behandlingId == behandling.id }
        val behandlingTilkommen = tilkommen.filter { it.behandlingId == behandling.id }

        TidslinjeBehandlingDto(
            id = behandling.id.value,
            status = behandling.status.tilTidslinjeBehandlingStatus(),
            fom = behandling.fom,
            tom = behandling.tom,
            skjæringstidspunkt = behandling.skjæringstidspunkt,
            revurdertAvBehandlingId = behandling.revurdertAvBehandlingId?.value,
            revurdererBehandlingId = behandling.revurdererBehandlingId?.value,
            yrkesaktiviteter =
                behandlingYrkesaktiviteter.map { yrkesaktivitet ->
                    TidlinjeYrkesaktivitetDto(
                        id = yrkesaktivitet.id.value,
                        sykmeldt = yrkesaktivitet.kategorisering.sykmeldt,
                        orgnummer = yrkesaktivitet.kategorisering.maybeOrgnummer(),
                        orgnavn = orgnavn(yrkesaktivitet.kategorisering.maybeOrgnummer()),
                        yrkesaktivitetType = yrkesaktivitet.kategorisering.tilYrkesaktivitetType(),
                    )
                },
            tilkommenInntekt =
                behandlingTilkommen.map { tilkommenInntekt ->
                    TidlinjeTilkommenInntektDto(
                        id = tilkommenInntekt.id.value,
                        orgnavn = orgnavn(tilkommenInntekt.ident),
                        ident = tilkommenInntekt.ident,
                        yrkesaktivitetType = tilkommenInntekt.yrkesaktivitetType.tilTilkommenInntektYrkesaktivitetType(),
                        fom = tilkommenInntekt.fom,
                        tom = tilkommenInntekt.tom,
                    )
                },
        )
    }

private fun YrkesaktivitetKategorisering.tilYrkesaktivitetType(): YrkesaktivitetTypeDto =
    when (this) {
        is YrkesaktivitetKategorisering.Arbeidstaker -> YrkesaktivitetTypeDto.ARBEIDSTAKER
        is YrkesaktivitetKategorisering.Frilanser -> YrkesaktivitetTypeDto.FRILANSER
        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> YrkesaktivitetTypeDto.SELVSTENDIG_NÆRINGSDRIVENDE
        is YrkesaktivitetKategorisering.Inaktiv -> YrkesaktivitetTypeDto.INAKTIV
        is YrkesaktivitetKategorisering.Arbeidsledig -> YrkesaktivitetTypeDto.ARBEIDSLEDIG
    }

private fun TilkommenInntektYrkesaktivitetType.tilTilkommenInntektYrkesaktivitetType(): TilkommenInntektYrkesaktivitetTypeDto =
    when (this) {
        TilkommenInntektYrkesaktivitetType.VIRKSOMHET -> TilkommenInntektYrkesaktivitetTypeDto.VIRKSOMHET
        TilkommenInntektYrkesaktivitetType.PRIVATPERSON -> TilkommenInntektYrkesaktivitetTypeDto.PRIVATPERSON
        TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE -> TilkommenInntektYrkesaktivitetTypeDto.NÆRINGSDRIVENDE
    }

private fun BehandlingStatus.tilTidslinjeBehandlingStatus(): TidslinjeBehandlingStatus =
    when (this) {
        UNDER_BEHANDLING -> TidslinjeBehandlingStatus.UNDER_BEHANDLING
        TIL_BESLUTNING -> TidslinjeBehandlingStatus.TIL_BESLUTNING
        UNDER_BESLUTNING -> TidslinjeBehandlingStatus.UNDER_BESLUTNING
        GODKJENT -> TidslinjeBehandlingStatus.GODKJENT
        REVURDERT -> TidslinjeBehandlingStatus.REVURDERT
    }

private fun TidslinjeData.orgnavn(orgnummer: String?): String? = organisasjonsnavnMap[orgnummer]?.navn

data class TidslinjeData(
    val behandlinger: List<Behandling>,
    val yrkesaktiviteter: List<Yrkesaktivitetsperiode>,
    val tilkommen: List<TilkommenInntekt>,
    val organisasjonsnavnMap: Map<String, Organisasjon?>,
)
