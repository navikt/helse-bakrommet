package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.SaksbehandlingsperiodeIkkeFunnetException

fun BehandlingDao.hentPeriode(
    ref: SaksbehandlingsperiodeReferanse,
    krav: BrukerHarRollePåSakenKrav?,
    måVæreUnderBehandling: Boolean = true,
): Behandling {
    val periode =
        this.finnBehandling(ref.periodeUUID)
            ?: throw SaksbehandlingsperiodeIkkeFunnetException()
    if (periode.spilleromPersonId != ref.spilleromPersonId.personId) {
        throw InputValideringException("Ugyldig saksbehandlingsperiode")
    }
    krav?.valider(periode)
    if (måVæreUnderBehandling && periode.status != BehandlingStatus.UNDER_BEHANDLING) {
        throw InputValideringException("Saksbehandlingsperiode er ikke under behandling")
    }
    return periode
}

fun BehandlingDao.reload(periode: Behandling) = finnBehandling(periode.id)!!

internal const val STATUS_UNDER_BEHANDLING_STR: String = "UNDER_BEHANDLING"
