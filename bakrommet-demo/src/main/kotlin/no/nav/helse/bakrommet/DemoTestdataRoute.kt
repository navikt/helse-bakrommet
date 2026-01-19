package no.nav.helse.bakrommet

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.testdata.alleTestdata
import no.nav.helse.bakrommet.testdata.scenarioer.alleScenarioer
import no.nav.helse.bakrommet.testdata.tilTestpersonForFrontend

fun Route.demoTestdataRoute() {
    get("/v1/demo/scenarioer") {
        call.respondText(alleScenarioer.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
    }

    get("/v1/demo/testpersoner") {
        val scenarioPersoner = alleScenarioer.map { it.testperson.fnr }.toSet()
        val testpersoner =
            alleTestdata.map { testperson ->
                testperson.tilTestpersonForFrontend(erScenarie = testperson.fnr in scenarioPersoner)
            }
        call.respondText(testpersoner.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
    }
}
