package no.nav.helse.bakrommet.testdata

import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.juli
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import java.time.LocalDate
import java.time.Year

data class Testperson(
    val fnr: String,
    val aktorId: String? = null,
    val spilleromId: String,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val fødselsdato: LocalDate = 17.juli(1997),
    val saksbehandingsperioder: List<Saksbehandingsperiode> = emptyList(),
    val soknader: List<SykepengesoknadDTO> = emptyList(),
    val inntektsmeldinger: List<Inntektsmelding>? = null,
    val sigrunData: Map<Year, String> = emptyMap(),
) {
    init {
        require(fnr.length == 11) { "Fnr skal være 11 siffer" }
        if (aktorId != null) {
            require(aktorId.length == 13) { "aktørid skal være 13 siffer" }
        }
    }
}

data class Saksbehandingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val søknadIder: List<String> = emptyList(),
)

fun Testperson.tilTestpersonForFrontend(erScenarie: Boolean = false): TestpersonForFrontend =
    TestpersonForFrontend(
        navn = this.fornavn + " " + this.etternavn,
        fnr = this.fnr,
        spilleromId = this.spilleromId,
        alder = 22,
        erScenarie = erScenarie,
    )

data class TestpersonForFrontend(
    val navn: String,
    val alder: Number,
    val spilleromId: String,
    val fnr: String,
    val erScenarie: Boolean,
)
