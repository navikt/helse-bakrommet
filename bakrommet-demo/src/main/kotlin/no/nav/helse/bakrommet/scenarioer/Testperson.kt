package no.nav.helse.bakrommet.scenarioer

import no.nav.helse.juli
import java.time.LocalDate

data class Testperson(
    val fnr: String,
    val aktorId: String? = null,
    val spilleromId: String? = null,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val fødselsdato: LocalDate = 17.juli(1997),
) {
    init {
        require(fnr.length == 11) { "Fnr skal være 11 siffer" }
        if (aktorId != null) {
            require(aktorId.length == 13) { "aktørid skal være 13 siffer" }
        }
    }
}
