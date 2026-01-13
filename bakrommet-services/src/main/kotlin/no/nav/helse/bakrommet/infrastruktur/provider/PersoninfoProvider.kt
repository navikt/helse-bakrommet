package no.nav.helse.bakrommet.infrastruktur.provider

import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import java.time.LocalDate

data class PdlIdent(
    val ident: String,
    val gruppe: String,
) {
    companion object {
        val FOLKEREGISTERIDENT = "FOLKEREGISTERIDENT"
        val AKTORID = "AKTORID"
        val NPID = "NPID"
    }
}

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
) {
    fun formattert(): String =
        when {
            mellomnavn.isNullOrBlank() -> "$fornavn $etternavn"
            else -> "$fornavn $mellomnavn $etternavn"
        }
}

data class PersonInfo(
    val navn: Navn,
    val fodselsdato: LocalDate?,
) {
    fun alder(): Int? {
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
}

interface PersoninfoProvider {
    suspend fun hentPersonInfo(
        saksbehandlerToken: AccessToken,
        ident: NaturligIdent,
    ): PersonInfo

    suspend fun hentIdenterFor(
        saksbehandlerToken: AccessToken,
        ident: NaturligIdent,
    ): List<PdlIdent>
}
