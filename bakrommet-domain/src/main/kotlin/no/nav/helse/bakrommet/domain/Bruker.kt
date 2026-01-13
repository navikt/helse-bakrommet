package no.nav.helse.bakrommet.domain

data class Bruker(
    val navn: String,
    val navIdent: String,
    val preferredUsername: String,
    val roller: Set<Rolle>,
)
