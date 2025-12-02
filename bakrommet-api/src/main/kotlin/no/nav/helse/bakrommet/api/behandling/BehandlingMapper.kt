package no.nav.helse.bakrommet.api.behandling

import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.behandling.SaksbehandlingsperiodeEndringDto
import no.nav.helse.bakrommet.api.dto.behandling.SaksbehandlingsperiodeEndringTypeDto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndring
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringType

fun Behandling.tilBehandlingDto(): BehandlingDto =
    BehandlingDto(
        id = id,
        spilleromPersonId = spilleromPersonId,
        opprettet = opprettet,
        opprettetAvNavIdent = opprettetAvNavIdent,
        opprettetAvNavn = opprettetAvNavn,
        fom = fom,
        tom = tom,
        status = status.tilTidslinjeBehandlingStatus(),
        beslutterNavIdent = beslutterNavIdent,
        skjæringstidspunkt = skjæringstidspunkt,
        individuellBegrunnelse = individuellBegrunnelse,
        sykepengegrunnlagId = sykepengegrunnlagId,
        revurdererSaksbehandlingsperiodeId = revurdererSaksbehandlingsperiodeId,
        revurdertAvBehandlingId = revurdertAvBehandlingId,
    )

private fun BehandlingStatus.tilTidslinjeBehandlingStatus(): TidslinjeBehandlingStatus =
    when (this) {
        BehandlingStatus.UNDER_BEHANDLING -> TidslinjeBehandlingStatus.UNDER_BEHANDLING
        BehandlingStatus.TIL_BESLUTNING -> TidslinjeBehandlingStatus.TIL_BESLUTNING
        BehandlingStatus.UNDER_BESLUTNING -> TidslinjeBehandlingStatus.UNDER_BESLUTNING
        BehandlingStatus.GODKJENT -> TidslinjeBehandlingStatus.GODKJENT
        BehandlingStatus.REVURDERT -> TidslinjeBehandlingStatus.REVURDERT
    }

fun SaksbehandlingsperiodeEndring.tilSaksbehandlingsperiodeEndringDto(): SaksbehandlingsperiodeEndringDto =
    SaksbehandlingsperiodeEndringDto(
        saksbehandlingsperiodeId = saksbehandlingsperiodeId,
        status = status.tilTidslinjeBehandlingStatus(),
        beslutterNavIdent = beslutterNavIdent,
        endretTidspunkt = endretTidspunkt,
        endretAvNavIdent = endretAvNavIdent,
        endringType = endringType.tilSaksbehandlingsperiodeEndringTypeDto(),
        endringKommentar = endringKommentar,
    )

private fun SaksbehandlingsperiodeEndringType.tilSaksbehandlingsperiodeEndringTypeDto(): SaksbehandlingsperiodeEndringTypeDto =
    when (this) {
        SaksbehandlingsperiodeEndringType.STARTET -> SaksbehandlingsperiodeEndringTypeDto.STARTET
        SaksbehandlingsperiodeEndringType.SENDT_TIL_BESLUTNING -> SaksbehandlingsperiodeEndringTypeDto.SENDT_TIL_BESLUTNING
        SaksbehandlingsperiodeEndringType.TATT_TIL_BESLUTNING -> SaksbehandlingsperiodeEndringTypeDto.TATT_TIL_BESLUTNING
        SaksbehandlingsperiodeEndringType.SENDT_I_RETUR -> SaksbehandlingsperiodeEndringTypeDto.SENDT_I_RETUR
        SaksbehandlingsperiodeEndringType.GODKJENT -> SaksbehandlingsperiodeEndringTypeDto.GODKJENT
        SaksbehandlingsperiodeEndringType.OPPDATERT_INDIVIDUELL_BEGRUNNELSE -> SaksbehandlingsperiodeEndringTypeDto.OPPDATERT_INDIVIDUELL_BEGRUNNELSE
        SaksbehandlingsperiodeEndringType.OPPDATERT_SKJÆRINGSTIDSPUNKT -> SaksbehandlingsperiodeEndringTypeDto.OPPDATERT_SKJÆRINGSTIDSPUNKT
        SaksbehandlingsperiodeEndringType.OPPDATERT_YRKESAKTIVITET_KATEGORISERING -> SaksbehandlingsperiodeEndringTypeDto.OPPDATERT_YRKESAKTIVITET_KATEGORISERING
        SaksbehandlingsperiodeEndringType.REVURDERING_STARTET -> SaksbehandlingsperiodeEndringTypeDto.REVURDERING_STARTET
    }
