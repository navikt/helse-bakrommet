package no.nav.helse.bakrommet.testdata

import no.nav.helse.bakrommet.testdata.scenarioer.alleScenarioer
import no.nav.helse.bakrommet.testdata.testpersoner.alleTestpersoner

val alleTestdata: List<Testperson> =
    mutableListOf<Testperson>().also { lista -> lista.addAll(alleScenarioer.map { it.testperson }) }.also { lista ->
        lista.addAll(alleTestpersoner)
    }
