package no.nav.helse.bakrommet.api.behandling

import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingEndringDto
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingEndringTypeDto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndring
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringType
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import java.time.OffsetDateTime
import java.time.ZoneId

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

fun Behandling.tilBehandlingDto(): BehandlingDto =
    BehandlingDto(
        id = id.value,
        naturligIdent = naturligIdent.value,
        opprettet = OffsetDateTime.ofInstant(opprettet, ZoneId.of("Europe/Oslo")),
        opprettetAvNavIdent = opprettetAvNavIdent,
        opprettetAvNavn = opprettetAvNavn,
        fom = fom,
        tom = tom,
        status = status.tilTidslinjeBehandlingStatus(),
        beslutterNavIdent = beslutterNavIdent,
        skjæringstidspunkt = skjæringstidspunkt,
        individuellBegrunnelse = individuellBegrunnelse,
        sykepengegrunnlagId = sykepengegrunnlagId,
        revurdererSaksbehandlingsperiodeId = revurdererBehandlingId?.value,
        revurdertAvBehandlingId = revurdertAvBehandlingId?.value,
    )

private fun BehandlingStatus.tilTidslinjeBehandlingStatus(): TidslinjeBehandlingStatus =
    when (this) {
        BehandlingStatus.UNDER_BEHANDLING -> TidslinjeBehandlingStatus.UNDER_BEHANDLING
        BehandlingStatus.TIL_BESLUTNING -> TidslinjeBehandlingStatus.TIL_BESLUTNING
        BehandlingStatus.UNDER_BESLUTNING -> TidslinjeBehandlingStatus.UNDER_BESLUTNING
        BehandlingStatus.GODKJENT -> TidslinjeBehandlingStatus.GODKJENT
        BehandlingStatus.REVURDERT -> TidslinjeBehandlingStatus.REVURDERT
    }

private fun no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.tilTidslinjeBehandlingStatus(): TidslinjeBehandlingStatus =
    when (this) {
        no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.UNDER_BEHANDLING -> TidslinjeBehandlingStatus.UNDER_BEHANDLING
        no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.TIL_BESLUTNING -> TidslinjeBehandlingStatus.TIL_BESLUTNING
        no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.UNDER_BESLUTNING -> TidslinjeBehandlingStatus.UNDER_BESLUTNING
        no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.GODKJENT -> TidslinjeBehandlingStatus.GODKJENT
        no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.REVURDERT -> TidslinjeBehandlingStatus.REVURDERT
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
