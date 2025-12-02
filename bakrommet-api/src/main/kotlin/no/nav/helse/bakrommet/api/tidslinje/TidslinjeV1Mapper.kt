package no.nav.helse.bakrommet.api.tidslinje

import no.nav.helse.bakrommet.api.dto.tidslinje.BehandlingStatusV1Dto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeElementV1Dto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeRadV1Dto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeV1Dto
import no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektTidslinjeElementV1Dto
import no.nav.helse.bakrommet.api.dto.tidslinje.YrkesaktivitetTidslinjeElementV1Dto
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.tidslinje.Tidslinje
import no.nav.helse.bakrommet.tidslinje.TidslinjeElement
import no.nav.helse.bakrommet.tidslinje.TidslinjeRad
import no.nav.helse.bakrommet.tidslinje.TilkommenInntektTidslinjeElement
import no.nav.helse.bakrommet.tidslinje.YrkesaktivitetTidslinjeElement

fun Tidslinje.tilTidslinjeV1Dto(): TidslinjeV1Dto =
    TidslinjeV1Dto(
        rader = rader.map { it.tilTidslinjeRadV1Dto() },
    )

private fun TidslinjeRad.tilTidslinjeRadV1Dto(): TidslinjeRadV1Dto =
    when (this) {
        is TidslinjeRad.OpprettetBehandling ->
            TidslinjeRadV1Dto.OpprettetBehandlingV1Dto(
                tidslinjeElementer = tidslinjeElementer.map { it.tilTidslinjeElementV1Dto() },
            )

        is TidslinjeRad.SykmeldtYrkesaktivitet ->
            TidslinjeRadV1Dto.SykmeldtYrkesaktivitetV1Dto(
                tidslinjeElementer = tidslinjeElementer.map { it.tilYrkesaktivitetTidslinjeElementV1Dto() },
                id = id,
                navn = navn,
            )

        is TidslinjeRad.TilkommenInntekt ->
            TidslinjeRadV1Dto.TilkommenInntektV1Dto(
                tidslinjeElementer = tidslinjeElementer.map { it.tilTilkommenInntektTidslinjeElementV1Dto() },
                id = id,
                navn = navn,
            )
    }

private fun TidslinjeElement.tilTidslinjeElementV1Dto(): TidslinjeElementV1Dto =
    TidslinjeElementV1Dto(
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingId = behandlingId,
        status = status.tilBehandlingStatusV1Dto(),
        historisk = historisk,
        revurdererBehandlingId = revurdererBehandlingId,
        revurdertAv = revurdertAv,
        historiske = historiske.map { it.tilTidslinjeElementV1Dto() },
    )

private fun YrkesaktivitetTidslinjeElement.tilYrkesaktivitetTidslinjeElementV1Dto(): YrkesaktivitetTidslinjeElementV1Dto =
    YrkesaktivitetTidslinjeElementV1Dto(
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingId = behandlingId,
        status = status.tilBehandlingStatusV1Dto(),
        historisk = historisk,
        revurdererBehandlingId = revurdererBehandlingId,
        revurdertAv = revurdertAv,
        yrkesaktivitetId = yrkesaktivitetId,
        ghost = ghost,
        historiske = historiske.map { it.tilYrkesaktivitetTidslinjeElementV1Dto() },
    )

private fun TilkommenInntektTidslinjeElement.tilTilkommenInntektTidslinjeElementV1Dto(): TilkommenInntektTidslinjeElementV1Dto =
    TilkommenInntektTidslinjeElementV1Dto(
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingId = behandlingId,
        status = status.tilBehandlingStatusV1Dto(),
        historisk = historisk,
        revurdererBehandlingId = revurdererBehandlingId,
        revurdertAv = revurdertAv,
        tilkommenInntektId = tilkommenInntektId,
        historiske = historiske.map { it.tilTilkommenInntektTidslinjeElementV1Dto() },
    )

private fun BehandlingStatus.tilBehandlingStatusV1Dto(): BehandlingStatusV1Dto =
    when (this) {
        BehandlingStatus.UNDER_BEHANDLING -> BehandlingStatusV1Dto.UNDER_BEHANDLING
        BehandlingStatus.TIL_BESLUTNING -> BehandlingStatusV1Dto.TIL_BESLUTNING
        BehandlingStatus.UNDER_BESLUTNING -> BehandlingStatusV1Dto.UNDER_BESLUTNING
        BehandlingStatus.GODKJENT -> BehandlingStatusV1Dto.GODKJENT
        BehandlingStatus.REVURDERT -> BehandlingStatusV1Dto.REVURDERT
    }
