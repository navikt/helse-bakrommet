package no.nav.helse.bakrommet.api.behandling

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.api.dto.behandling.CreatePeriodeRequestDto
import no.nav.helse.bakrommet.api.dto.behandling.OppdaterSkjæringstidspunktRequestDto
import no.nav.helse.bakrommet.api.dto.behandling.SendTilBeslutningRequestDto
import no.nav.helse.bakrommet.api.dto.behandling.SendTilbakeRequestDto
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingService
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.periodeUUID
import no.nav.helse.bakrommet.personId
import no.nav.helse.bakrommet.util.sikkerLogger
import java.time.LocalDate

fun RoutingCall.periodeReferanse() =
    no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeReferanse(
        spilleromPersonId = personId(),
        periodeUUID = periodeUUID(),
    )

fun Route.behandlingRoute(service: BehandlingService) {
    route("/v1/behandlinger") {
        get {
            val perioder = service.hentAlleSaksbehandlingsperioder()
            call.respondJson(perioder.map { it.tilBehandlingDto() })
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger") {
        /** Opprett en ny periode */
        post {
            val body = call.receive<CreatePeriodeRequestDto>()
            val nyPeriode =
                service.opprettNySaksbehandlingsperiode(
                    spilleromPersonId = call.personId(),
                    fom = LocalDate.parse(body.fom),
                    tom = LocalDate.parse(body.tom),
                    søknader = body.søknader?.toSet() ?: emptySet(),
                    saksbehandler = call.saksbehandlerOgToken(),
                )
            call.respondJson(nyPeriode.tilBehandlingDto(), status = HttpStatusCode.Created)
        }

        /** Hent alle perioder for en person */
        get {
            service.finnPerioderForPerson(call.personId()).let { perioder ->
                call.respondJson(perioder.map { it.tilBehandlingDto() })
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}") {
        get {
            service.hentPeriode(call.periodeReferanse()).let {
                call.respondJson(it.tilBehandlingDto())
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/historikk") {
        get {
            val historikk = service.hentHistorikkFor(call.periodeReferanse())
            call.respondJson(historikk.map { it.tilSaksbehandlingsperiodeEndringDto() })
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/sendtilbeslutning") {
        post {
            val body = call.receive<SendTilBeslutningRequestDto>()
            service
                .sendTilBeslutning(call.periodeReferanse(), body.individuellBegrunnelse, call.saksbehandler())
                .let { oppdatertPeriode ->
                    call.respondJson(oppdatertPeriode.tilBehandlingDto())
                }
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/tatilbeslutning") {
        post {
            service.taTilBeslutning(call.periodeReferanse(), call.saksbehandler()).let { oppdatertPeriode ->
                call.respondJson(oppdatertPeriode.tilBehandlingDto())
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/sendtilbake") {
        post {
            val kommentar =
                try {
                    call.receive<SendTilbakeRequestDto>().kommentar
                } catch (ex: io.ktor.server.plugins.BadRequestException) {
                    sikkerLogger.warn("Klarte ikke parse SendTilbakeRequest", ex)
                    throw InputValideringException("Ugyldig innhold i POST-body")
                }
            service
                .sendTilbakeFraBeslutning(call.periodeReferanse(), call.saksbehandler(), kommentar = kommentar)
                .let { oppdatertPeriode ->
                    call.respondJson(oppdatertPeriode.tilBehandlingDto())
                }
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/godkjenn") {
        post {
            service.godkjennPeriode(call.periodeReferanse(), call.saksbehandler()).let { oppdatertPeriode ->
                call.respondJson(oppdatertPeriode.tilBehandlingDto())
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/revurder") {
        post {
            service.revurderPeriode(call.periodeReferanse(), call.saksbehandler()).let { oppdatertPeriode ->
                call.respondJson(oppdatertPeriode.tilBehandlingDto(), status = HttpStatusCode.Created)
            }
        }
    }

    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/skjaeringstidspunkt") {
        put {
            val body = call.receive<OppdaterSkjæringstidspunktRequestDto>()
            val skjæringstidspunkt = body.skjaeringstidspunkt.let { LocalDate.parse(it) }
            service
                .oppdaterSkjæringstidspunkt(
                    periodeRef = call.periodeReferanse(),
                    skjæringstidspunkt = skjæringstidspunkt,
                    saksbehandler = call.saksbehandler(),
                ).let { oppdatertPeriode ->
                    call.respondJson(oppdatertPeriode.tilBehandlingDto())
                }
        }
    }
}
