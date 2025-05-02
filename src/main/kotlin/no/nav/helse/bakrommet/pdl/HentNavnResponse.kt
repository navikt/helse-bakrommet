package no.nav.helse.bakrommet.pdl

data class HentNavnResponseData(
    val hentPerson: HentNavn? = null,
)

data class HentNavn(
    val navn: List<Navn>? = null,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)
