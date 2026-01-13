package no.nav.helse.bakrommet.api.behandling

import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingEndringDto
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingEndringTypeDto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndring
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringType

fun BehandlingDbRecord.tilBehandlingDto(): BehandlingDto =
    BehandlingDto(
        id = id,
        naturligIdent = naturligIdent.value,
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

fun SaksbehandlingsperiodeEndring.tilSaksbehandlingsperiodeEndringDto(): BehandlingEndringDto =
    BehandlingEndringDto(
        behandlingId = behandlingId,
        status = status.tilTidslinjeBehandlingStatus(),
        beslutterNavIdent = beslutterNavIdent,
        endretTidspunkt = endretTidspunkt,
        endretAvNavIdent = endretAvNavIdent,
        endringType = endringType.tilSaksbehandlingsperiodeEndringTypeDto(),
        endringKommentar = endringKommentar,
    )

private fun SaksbehandlingsperiodeEndringType.tilSaksbehandlingsperiodeEndringTypeDto(): BehandlingEndringTypeDto =
    when (this) {
        SaksbehandlingsperiodeEndringType.STARTET -> BehandlingEndringTypeDto.STARTET
        SaksbehandlingsperiodeEndringType.SENDT_TIL_BESLUTNING -> BehandlingEndringTypeDto.SENDT_TIL_BESLUTNING
        SaksbehandlingsperiodeEndringType.TATT_TIL_BESLUTNING -> BehandlingEndringTypeDto.TATT_TIL_BESLUTNING
        SaksbehandlingsperiodeEndringType.SENDT_I_RETUR -> BehandlingEndringTypeDto.SENDT_I_RETUR
        SaksbehandlingsperiodeEndringType.GODKJENT -> BehandlingEndringTypeDto.GODKJENT
        SaksbehandlingsperiodeEndringType.OPPDATERT_INDIVIDUELL_BEGRUNNELSE -> BehandlingEndringTypeDto.OPPDATERT_INDIVIDUELL_BEGRUNNELSE
        SaksbehandlingsperiodeEndringType.OPPDATERT_SKJÆRINGSTIDSPUNKT -> BehandlingEndringTypeDto.OPPDATERT_SKJÆRINGSTIDSPUNKT
        SaksbehandlingsperiodeEndringType.OPPDATERT_YRKESAKTIVITET_KATEGORISERING -> BehandlingEndringTypeDto.OPPDATERT_YRKESAKTIVITET_KATEGORISERING
        SaksbehandlingsperiodeEndringType.REVURDERING_STARTET -> BehandlingEndringTypeDto.REVURDERING_STARTET
    }
