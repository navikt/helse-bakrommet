package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.periodeUUID
import no.nav.helse.bakrommet.personId
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.LocalDate
import java.util.*

fun RoutingCall.periodeReferanse() =
    SaksbehandlingsperiodeReferanse(
        spilleromPersonId = personId(),
        periodeUUID = periodeUUID(),
    )

internal fun Route.saksbehandlingsperiodeRoute(service: SaksbehandlingsperiodeService) {
    route("/v1/saksbehandlingsperioder") {
        get {
            val perioder = service.hentAlleSaksbehandlingsperioder()
            call.respondPerioder(perioder)
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder") {
        data class CreatePeriodeRequest(
            val fom: String,
            val tom: String,
            val søknader: List<UUID>? = null,
        )

        /** Opprett en ny periode */
        post {
            val body = call.receive<CreatePeriodeRequest>()
            val nyPeriode =
                service.opprettNySaksbehandlingsperiode(
                    spilleromPersonId = call.personId(),
                    fom = LocalDate.parse(body.fom),
                    tom = LocalDate.parse(body.tom),
                    søknader = body.søknader?.toSet() ?: emptySet(),
                    saksbehandler = call.saksbehandlerOgToken(),
                )
            call.respondPeriode(nyPeriode, HttpStatusCode.Created)
        }

        /** Hent alle perioder for en person */
        get {
            service.finnPerioderForPerson(call.personId()).let { perioder ->
                call.respondPerioder(perioder)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}") {
        get {
            service.hentPeriode(call.periodeReferanse()).let {
                call.respondPeriode(it)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/historikk") {
        get {
            val historikk = service.hentHistorikkFor(call.periodeReferanse())
            call.respondText(historikk.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/sendtilbeslutning") {
        post {
            service.sendTilBeslutning(call.periodeReferanse(), call.saksbehandler()).let { oppdatertPeriode ->
                call.respondPeriode(oppdatertPeriode)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/tatilbeslutning") {
        post {
            service.taTilBeslutning(call.periodeReferanse(), call.saksbehandler()).let { oppdatertPeriode ->
                call.respondPeriode(oppdatertPeriode)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/sendtilbake") {
        post {
            val kommentar = "Ikke bra" // TODO
            service.sendTilbakeFraBeslutning(call.periodeReferanse(), call.saksbehandler(), kommentar = kommentar).let { oppdatertPeriode ->
                call.respondPeriode(oppdatertPeriode)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/godkjenn") {
        post {
            service.godkjennPeriode(call.periodeReferanse(), call.saksbehandler()).let { oppdatertPeriode ->
                call.respondPeriode(oppdatertPeriode)
            }
        }
    }
}

private suspend fun RoutingCall.respondPeriode(
    periode: Saksbehandlingsperiode,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondText(periode.serialisertTilString(), ContentType.Application.Json, status)
}

private suspend fun RoutingCall.respondPerioder(
    perioder: List<Saksbehandlingsperiode>,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondText(perioder.serialisertTilString(), ContentType.Application.Json, status)
}
