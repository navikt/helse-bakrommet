package no.nav.helse.bakrommet.api.tidslinje

import no.nav.helse.bakrommet.api.dto.tidslinje.TidlinjeTilkommenInntektDto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidlinjeYrkesaktivitetDto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingDto
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.maybeOrgnummer
import no.nav.helse.bakrommet.tidslinje.TidslinjeData

fun TidslinjeData.tilTidslinjeDto(): List<TidslinjeBehandlingDto> =
    behandlinger.map { behandling ->
        val behandlingYrkesaktiviteter = yrkesaktiviteter.filter { it.behandlingId == behandling.id }
        val behandlingTilkommen = tilkommen.filter { it.behandlingId == behandling.id }

        TidslinjeBehandlingDto(
            id = behandling.id,
            status = behandling.status.tilTidslinjeBehandlingStatus(),
            fom = behandling.fom,
            tom = behandling.tom,
            skjæringstidspunkt = behandling.skjæringstidspunkt,
            revurdertAvBehandlingId = behandling.revurdertAvBehandlingId,
            revurdererBehandlingId = behandling.revurdererSaksbehandlingsperiodeId,
            yrkesaktiviteter =
                behandlingYrkesaktiviteter.map { yrkesaktivitet ->
                    TidlinjeYrkesaktivitetDto(
                        id = yrkesaktivitet.id,
                        sykmeldt = yrkesaktivitet.kategorisering.sykmeldt,
                        orgnummer = yrkesaktivitet.kategorisering.maybeOrgnummer(),
                        orgnavn = orgnavn(yrkesaktivitet.kategorisering.maybeOrgnummer()),
                        yrkesaktivitetType = yrkesaktivitet.kategorisering.tilYrkesaktivitetType(),
                    )
                },
            tilkommenInntekt =
                behandlingTilkommen.map { tilkommenInntekt ->
                    TidlinjeTilkommenInntektDto(
                        id = tilkommenInntekt.id,
                        orgnavn = orgnavn(tilkommenInntekt.tilkommenInntekt.ident),
                        ident = tilkommenInntekt.tilkommenInntekt.ident,
                        yrkesaktivitetType = tilkommenInntekt.tilkommenInntekt.yrkesaktivitetType.tilTilkommenInntektYrkesaktivitetType(),
                        fom = tilkommenInntekt.tilkommenInntekt.fom,
                        tom = tilkommenInntekt.tilkommenInntekt.tom,
                    )
                },
        )
    }

private fun YrkesaktivitetKategorisering.tilYrkesaktivitetType(): no.nav.helse.bakrommet.api.dto.tidslinje.YrkesaktivitetType =
    when (this) {
        is YrkesaktivitetKategorisering.Arbeidstaker -> no.nav.helse.bakrommet.api.dto.tidslinje.YrkesaktivitetType.ARBEIDSTAKER
        is YrkesaktivitetKategorisering.Frilanser -> no.nav.helse.bakrommet.api.dto.tidslinje.YrkesaktivitetType.FRILANSER
        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> no.nav.helse.bakrommet.api.dto.tidslinje.YrkesaktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE
        is YrkesaktivitetKategorisering.Inaktiv -> no.nav.helse.bakrommet.api.dto.tidslinje.YrkesaktivitetType.INAKTIV
        is YrkesaktivitetKategorisering.Arbeidsledig -> no.nav.helse.bakrommet.api.dto.tidslinje.YrkesaktivitetType.ARBEIDSLEDIG
    }

private fun TilkommenInntektYrkesaktivitetType.tilTilkommenInntektYrkesaktivitetType(): no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetType =
    when (this) {
        TilkommenInntektYrkesaktivitetType.VIRKSOMHET -> no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetType.VIRKSOMHET
        TilkommenInntektYrkesaktivitetType.PRIVATPERSON -> no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetType.PRIVATPERSON
        TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE -> no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE
    }

private fun BehandlingStatus.tilTidslinjeBehandlingStatus(): no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus =
    when (this) {
        BehandlingStatus.UNDER_BEHANDLING -> no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus.UNDER_BEHANDLING
        BehandlingStatus.TIL_BESLUTNING -> no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus.TIL_BESLUTNING
        BehandlingStatus.UNDER_BESLUTNING -> no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus.UNDER_BESLUTNING
        BehandlingStatus.GODKJENT -> no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus.GODKJENT
        BehandlingStatus.REVURDERT -> no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus.REVURDERT
    }

fun TidslinjeData.orgnavn(orgnummer: String?): String? = organisasjonsnavnMap[orgnummer]?.navn
