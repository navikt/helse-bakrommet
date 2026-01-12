package no.nav.helse.bakrommet.pdl

import no.nav.helse.bakrommet.infrastruktur.provider.Navn
import java.time.LocalDate

data class HentPersonResponseData(
    val hentPerson: HentPerson? = null,
)

data class HentPerson(
    val navn: List<Navn>? = null,
    val foedselsdato: List<Foedselsdato>? = null,
)

data class Foedselsdato(
    val foedselsdato: LocalDate,
)
