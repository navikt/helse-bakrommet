package no.nav.helse.bakrommet.testdata.scenarioer

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.bakrommet.testdata.Testperson
import no.nav.helse.bakrommet.testdata.TestpersonForFrontend
import no.nav.helse.bakrommet.testdata.tilTestpersonForFrontend

data class Testscenario(
    @JsonIgnore
    val testperson: Testperson,
    val beskrivelse: String,
    val tittel: String,
) {
    @JsonSerialize
    fun testperson(): TestpersonForFrontend = testperson.tilTestpersonForFrontend()
}
