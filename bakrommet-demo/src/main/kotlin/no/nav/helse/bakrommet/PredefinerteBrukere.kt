package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.Rolle

val saksMcBehandlersen =
    Bruker(
        navn = "Saks McBehandlersen",
        navIdent = "Z123456",
        preferredUsername = "saks.mcbehandlersen@nav.no",
        roller = setOf(Rolle.SAKSBEHANDLER),
    )
val gullbart =
    Bruker(
        navn = "Gunnar Gullbart",
        navIdent = "G123456",
        preferredUsername = "gunnar.gullbart@nav.no",
        roller = setOf(Rolle.SAKSBEHANDLER),
    )
val beritBeslutter =
    Bruker(
        navn = "Berit Beslutter",
        navIdent = "B456789",
        preferredUsername = "berit.beslutter@nav.no",
        roller = setOf(Rolle.BESLUTTER),
    )
val predefinerteBrukere =
    listOf(
        saksMcBehandlersen,
        gullbart,
        Bruker(
            navn = "Vetle Veileder",
            navIdent = "V987654",
            preferredUsername = "vetle.veileder@nav.no",
            roller = setOf(Rolle.LES),
        ),
        beritBeslutter,
        Bruker(
            navn = "Kai Kombinator",
            navIdent = "K111222",
            preferredUsername = "kai.kombinator@nav.no",
            roller = setOf(Rolle.SAKSBEHANDLER, Rolle.BESLUTTER),
        ),
    )
