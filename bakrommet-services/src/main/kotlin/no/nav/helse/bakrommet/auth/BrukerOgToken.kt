package no.nav.helse.bakrommet.auth

import no.nav.helse.bakrommet.domain.Bruker

class BrukerOgToken(
    val bruker: Bruker,
    val token: SpilleromBearerToken,
)
