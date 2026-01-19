package no.nav.helse.bakrommet.testdata

import no.nav.helse.bakrommet.aareg.Arbeidsforhold
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektRequest
import no.nav.helse.bakrommet.infrastruktur.provider.Inntektsinformasjon
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.juli
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import java.time.LocalDate
import java.time.Year
import java.util.UUID

data class Testperson(
    val fnr: String,
    val pseudoId: UUID = UUID.nameUUIDFromBytes(fnr.toByteArray()),
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val fødselsdato: LocalDate = 17.juli(1997),
    val behandlinger: List<Behandling> = emptyList(),
    val soknader: List<SykepengesoknadDTO> = emptyList(),
    val inntektsmeldinger: List<Inntektsmelding>? = null,
    val sigrunData: Map<Year, String> = emptyMap(),
    val aaregData: List<Arbeidsforhold>? = null,
    val ainntektData: List<Inntektsinformasjon>? = null,
) {
    init {
        require(fnr.length == 11) { "Fnr skal være 11 siffer" }
    }
}

data class Behandling(
    val fom: LocalDate,
    val tom: LocalDate,
    val søknadIder: Set<UUID> = emptySet(),
    val avsluttet: Boolean = false,
    val inntektRequest: InntektRequest? = null,
)

fun Testperson.tilTestpersonForFrontend(erScenarie: Boolean = false): TestpersonForFrontend =
    TestpersonForFrontend(
        navn = this.fornavn + " " + this.etternavn,
        fnr = this.fnr,
        spilleromId = this.pseudoId,
        alder = this.fødselsdato.until(LocalDate.now()).years,
        erScenarie = erScenarie,
    )

data class TestpersonForFrontend(
    val navn: String,
    val alder: Number,
    val spilleromId: UUID,
    val fnr: String,
    val erScenarie: Boolean,
)
