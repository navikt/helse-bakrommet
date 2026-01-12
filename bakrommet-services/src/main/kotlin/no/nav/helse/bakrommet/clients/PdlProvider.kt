package no.nav.helse.bakrommet.clients

import no.nav.helse.bakrommet.auth.SpilleromBearerToken
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

data class PersonInfo(
    val navn: Navn,
    val fodselsdato: LocalDate?,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

interface PdlProvider {
    suspend fun hentIdenterFor(
        saksbehandlerToken: SpilleromBearerToken,
        ident: String,
    ): List<PdlIdent>

    suspend fun hentPersonInfo(
        saksbehandlerToken: SpilleromBearerToken,
        ident: String,
    ): PersonInfo
}
