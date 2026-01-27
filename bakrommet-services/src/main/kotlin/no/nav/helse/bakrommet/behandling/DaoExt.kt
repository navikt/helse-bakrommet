package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.SaksbehandlingsperiodeIkkeFunnetException

fun BehandlingDao.hentPeriode(
    ref: BehandlingReferanse,
    krav: BrukerHarRollePåSakenKrav?,
    måVæreUnderBehandling: Boolean = true,
): BehandlingDbRecord {
    val periode =
        this.finnBehandling(ref.behandlingId)
            ?: throw SaksbehandlingsperiodeIkkeFunnetException()
    if (periode.naturligIdent != ref.naturligIdent) {
        throw InputValideringException("Ugyldig saksbehandlingsperiode")
    }
    krav?.valider(periode)
    if (måVæreUnderBehandling && periode.status != BehandlingStatus.UNDER_BEHANDLING) {
        throw InputValideringException("Saksbehandlingsperiode er ikke under behandling")
    }
    return periode
}

fun BehandlingDao.reload(periode: BehandlingDbRecord) = finnBehandling(periode.id)!!
