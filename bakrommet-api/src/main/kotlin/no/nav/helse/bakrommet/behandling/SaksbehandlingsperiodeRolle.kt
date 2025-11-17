package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.ForbiddenException

private fun krevAtBrukerErBeslutterFor(
    bruker: Bruker,
    periode: Behandling,
) {
    fun Bruker.erBeslutterFor(periode: Behandling): Boolean = periode.beslutterNavIdent == this.navIdent

    if (!bruker.erBeslutterFor(periode)) {
        throw ForbiddenException("Ikke beslutter for periode")
    }
}

private fun krevAtBrukerErSaksbehandlerFor(
    bruker: Bruker,
    periode: Behandling,
) {
    fun Bruker.erSaksbehandlerFor(periode: Behandling): Boolean = periode.opprettetAvNavIdent == this.navIdent

    if (!bruker.erSaksbehandlerFor(periode)) {
        throw ForbiddenException("Ikke saksbehandler for periode")
    }
}

class BrukerHarRollePåSakenKrav(
    private val bruker: Bruker,
    private val valideringsfunksjon: (Bruker, Behandling) -> Unit,
) {
    fun valider(periode: Behandling) {
        valideringsfunksjon(bruker, periode)
    }
}

fun Bruker.erSaksbehandlerPåSaken() = BrukerHarRollePåSakenKrav(this, ::krevAtBrukerErSaksbehandlerFor)

fun Bruker.erBeslutterPåSaken() = BrukerHarRollePåSakenKrav(this, ::krevAtBrukerErBeslutterFor)
