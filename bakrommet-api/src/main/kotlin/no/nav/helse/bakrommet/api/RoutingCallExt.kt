package no.nav.helse.bakrommet.api

import io.ktor.server.routing.RoutingCall
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetId
import no.nav.helse.bakrommet.errorhandling.PersonIkkeFunnetException
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.person.PseudoId
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.util.UUID

fun RoutingCall.personPseudoId(): UUID = pseudoId().value

fun RoutingCall.pseudoId(): PseudoId =
    PseudoId(
        parameters[PARAM_PSEUDO_ID]?.somGyldigUUID()
            ?: error("Mangler personId i path"),
    )

fun RoutingCall.behandlingIdLegacy(): UUID = behandlingId().value

suspend fun RoutingCall.naturligIdent(personService: PersonService): NaturligIdent {
    val personPseudoId = personPseudoId()
    return personService.finnNaturligIdent(personPseudoId)
        ?: throw PersonIkkeFunnetException()
}

suspend fun RoutingCall.periodeReferanse(personService: PersonService) =
    BehandlingReferanse(
        naturligIdent = naturligIdent(personService),
        behandlingId = behandlingIdLegacy(),
    )

fun RoutingCall.behandlingId() =
    BehandlingId(
        value =
            parameters[PARAM_BEHANDLING_ID]?.somGyldigUUID()
                ?: error("Mangler behandlingId i path"),
    )

suspend fun RoutingCall.tilkommenInntektReferanse(personService: PersonService) =
    TilkommenInntektReferanse(
        behandling = periodeReferanse(personService),
        tilkommenInntektId = parameters[PARAM_TILKOMMENINNTEKT_ID].somGyldigUUID(),
    )

suspend fun RoutingCall.yrkesaktivitetReferanse(personService: PersonService): YrkesaktivitetReferanse =
    YrkesaktivitetReferanse(
        behandlingReferanse = periodeReferanse(personService),
        yrkesaktivitetUUID = parameters[PARAM_YRKESAKTIVITETUUID].somGyldigUUID(),
    )

fun RoutingCall.yrkesaktivitetId(): YrkesaktivitetId = YrkesaktivitetId(parameters[PARAM_YRKESAKTIVITETUUID].somGyldigUUID())
