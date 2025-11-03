package no.nav.helse.bakrommet

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.testdata.alleTestdata
import no.nav.helse.bakrommet.testdata.scenarioer.alleScenarioer
import no.nav.helse.bakrommet.testdata.tilTestpersonForFrontend
import no.nav.helse.bakrommet.util.serialisertTilString

fun Route.demoTestdataRoute() {
    get("/v1/demo/scenarioer") {
        call.respondText(alleScenarioer.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
    }

    get("/v1/demo/testpersoner") {
        call.respondText(alleTestdata().map { it.tilTestpersonForFrontend() }.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
    }
}
