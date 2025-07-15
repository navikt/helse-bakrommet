package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.SaksbehandlingsperiodeIkkeFunnetException

fun SaksbehandlingsperiodeDao.hentPeriode(ref: SaksbehandlingsperiodeReferanse): Saksbehandlingsperiode {
    val periode =
        this.finnSaksbehandlingsperiode(ref.periodeUUID)
            ?: throw SaksbehandlingsperiodeIkkeFunnetException()
    if (periode.spilleromPersonId != ref.spilleromPersonId.personId) {
        throw InputValideringException("Ugyldig saksbehandlingsperiode")
    }
    return periode
}

fun SaksbehandlingsperiodeDao.reload(periode: Saksbehandlingsperiode) = finnSaksbehandlingsperiode(periode.id)!!
