package no.nav.helse.bakrommet.api.validering

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.api.PARAM_BEHANDLING_ID
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.periodeReferanse
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.behandling.validering.ValideringService
import no.nav.helse.bakrommet.person.PersonService

fun Route.valideringRoute(
    valideringService: ValideringService,
    personService: PersonService,
) {
    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/validering") {
        get {
            val inkluderSluttvalidering = call.request.queryParameters["inkluderSluttvalidering"]?.toBoolean() ?: false
            val resultat = valideringService.valider(call.periodeReferanse(personService), inkluderSluttvalidering)
            call.respondJson(resultat.map { it.tilValideringDto() })
        }
    }
}
