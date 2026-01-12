package no.nav.helse.bakrommet.auth

data class Bruker(
    val navn: String,
    val navIdent: String,
    val preferredUsername: String,
    val roller: Set<Rolle>,
)

class BrukerOgToken(
    val bruker: Bruker,
    val token: SpilleromBearerToken,
)
