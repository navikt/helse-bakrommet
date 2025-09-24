package no.nav.helse.bakrommet.pdl

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

data class PersonInfo(
    val navn: Navn,
    val fodselsdato: LocalDate?,
)

fun PersonInfo.alder(): Int? {
    val today = LocalDate.now()
    if (fodselsdato == null) {
        return null
    }
    val age = today.year - fodselsdato.year
    if (today.monthValue < fodselsdato.monthValue || (today.monthValue == fodselsdato.monthValue && today.dayOfMonth < fodselsdato.dayOfMonth)) {
        return age - 1
    }
    return age
}
