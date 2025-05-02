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

fun Navn.formattert(): String {
    return when {
        mellomnavn.isNullOrBlank() -> "$fornavn $etternavn"
        else -> "$fornavn $mellomnavn $etternavn"
    }
}
