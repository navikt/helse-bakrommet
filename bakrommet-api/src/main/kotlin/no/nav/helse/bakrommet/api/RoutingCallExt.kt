package no.nav.helse.bakrommet.api

import io.ktor.server.routing.RoutingCall
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.PersonIkkeFunnetException
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.util.UUID

fun RoutingCall.personPseudoId(): UUID =
    parameters[PARAM_PERSONID]?.somGyldigUUID()
        ?: throw InputValideringException("Mangler personId i path")

fun RoutingCall.behandlingId(): UUID =
    parameters[PARAM_PERIODEUUID]?.somGyldigUUID()
        ?: throw InputValideringException("Mangler behandlingId i path")

suspend fun RoutingCall.naturligIdent(personService: PersonService): NaturligIdent {
    val personPseudoId = personPseudoId()
    return personService.finnNaturligIdent(personPseudoId)
        ?: throw PersonIkkeFunnetException()
}

suspend fun RoutingCall.periodeReferanse(personService: PersonService) =
    SaksbehandlingsperiodeReferanse(
        naturligIdent = naturligIdent(personService),
        behandlingId = behandlingId(),
    )

suspend fun RoutingCall.tilkommenInntektReferanse(personService: PersonService) =
    TilkommenInntektReferanse(
        behandling = periodeReferanse(personService),
        tilkommenInntektId = parameters[PARAM_TILKOMMENINNTEKT_ID].somGyldigUUID(),
    )

suspend fun RoutingCall.yrkesaktivitetReferanse(personService: PersonService): YrkesaktivitetReferanse =
    YrkesaktivitetReferanse(
        saksbehandlingsperiodeReferanse = periodeReferanse(personService),
        yrkesaktivitetUUID = parameters[PARAM_YRKESAKTIVITETUUID].somGyldigUUID(),
    )
