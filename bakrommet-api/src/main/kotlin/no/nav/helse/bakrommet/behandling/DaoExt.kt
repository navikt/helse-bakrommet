package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.SaksbehandlingsperiodeIkkeFunnetException

fun SaksbehandlingsperiodeDao.hentPeriode(
    ref: SaksbehandlingsperiodeReferanse,
    krav: BrukerHarRollePåSakenKrav?,
): Saksbehandlingsperiode {
    val periode =
        this.finnSaksbehandlingsperiode(ref.periodeUUID)
            ?: throw SaksbehandlingsperiodeIkkeFunnetException()
    if (periode.spilleromPersonId != ref.spilleromPersonId.personId) {
        throw InputValideringException("Ugyldig saksbehandlingsperiode")
    }
    krav?.valider(periode)
    return periode
}

fun SaksbehandlingsperiodeDao.reload(periode: Saksbehandlingsperiode) = finnSaksbehandlingsperiode(periode.id)!!
