package no.nav.helse.bakrommet.api.behandling

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.PARAM_BEHANDLING_ID
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.auth.bruker
import no.nav.helse.bakrommet.api.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.api.dto.behandling.OppdaterSkjæringstidspunktRequestDto
import no.nav.helse.bakrommet.api.dto.behandling.OpprettBehandlingRequestDto
import no.nav.helse.bakrommet.api.dto.behandling.SendTilBeslutningRequestDto
import no.nav.helse.bakrommet.api.dto.behandling.SendTilbakeRequestDto
import no.nav.helse.bakrommet.api.naturligIdent
import no.nav.helse.bakrommet.api.periodeReferanse
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.behandling.BehandlingService
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.sikkerLogger
import java.time.LocalDate

fun Route.behandlingRoute(
    service: BehandlingService,
    personService: PersonService,
    db: DbDaoer<AlleDaoer>,
) {
    route("/v1/behandlinger") {
        get {
            val perioder = service.hentAlleSaksbehandlingsperioder()
            call.respondJson(perioder.map { it.tilBehandlingDto() })
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger") {
        post {
            val body = call.receive<OpprettBehandlingRequestDto>()
            val naturligIDent = call.naturligIdent(personService)
            val nyPeriode =
                service.opprettNyBehandling(
                    naturligIdent = naturligIDent,
                    fom = body.fom,
                    tom = body.tom,
                    søknader = body.søknader?.toSet() ?: emptySet(),
                    saksbehandler = call.saksbehandlerOgToken(),
                )
            call.respondJson(nyPeriode.tilBehandlingDto(), status = HttpStatusCode.Created)
        }

        get {
            service.finnPerioderForPerson(call.naturligIdent(personService)).let { perioder ->
                call.respondJson(perioder.map { it.tilBehandlingDto() })
            }
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}") {
        get {
            db
                .transactional {
                    hentOgVerifiserBehandling(call)
                }.let {
                    call.respondJson(it.tilBehandlingDto())
                }
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/historikk") {
        get {
            val historikk = service.hentHistorikkFor(call.periodeReferanse(personService))
            call.respondJson(historikk.map { it.tilSaksbehandlingsperiodeEndringDto() })
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/sendtilbeslutning") {
        post {
            val body = call.receive<SendTilBeslutningRequestDto>()
            service
                .sendTilBeslutning(call.periodeReferanse(personService), body.individuellBegrunnelse, call.bruker())
                .let { oppdatertPeriode ->
                    call.respondJson(oppdatertPeriode.tilBehandlingDto())
                }
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/tatilbeslutning") {
        post {
            service.taTilBeslutning(call.periodeReferanse(personService), call.bruker()).let { oppdatertPeriode ->
                call.respondJson(oppdatertPeriode.tilBehandlingDto())
            }
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/sendtilbake") {
        post {
            val kommentar =
                try {
                    call.receive<SendTilbakeRequestDto>().kommentar
                } catch (ex: io.ktor.server.plugins.BadRequestException) {
                    sikkerLogger.warn("Klarte ikke parse SendTilbakeRequest", ex)
                    throw InputValideringException("Ugyldig innhold i POST-body")
                }
            service
                .sendTilbakeFraBeslutning(call.periodeReferanse(personService), call.bruker(), kommentar = kommentar)
                .let { oppdatertPeriode ->
                    call.respondJson(oppdatertPeriode.tilBehandlingDto())
                }
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/godkjenn") {
        post {
            service.godkjennPeriode(call.periodeReferanse(personService), call.bruker()).let { oppdatertPeriode ->
                call.respondJson(oppdatertPeriode.tilBehandlingDto())
            }
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/revurder") {
        post {
            service.revurderPeriode(call.periodeReferanse(personService), call.bruker()).let { oppdatertPeriode ->
                call.respondJson(oppdatertPeriode.tilBehandlingDto(), status = HttpStatusCode.Created)
            }
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/skjaeringstidspunkt") {
        put {
            val body = call.receive<OppdaterSkjæringstidspunktRequestDto>()
            val skjæringstidspunkt = body.skjaeringstidspunkt.let { LocalDate.parse(it) }
            service
                .oppdaterSkjæringstidspunkt(
                    periodeRef = call.periodeReferanse(personService),
                    skjæringstidspunkt = skjæringstidspunkt,
                    saksbehandler = call.bruker(),
                ).let { oppdatertPeriode ->
                    call.respondJson(oppdatertPeriode.tilBehandlingDto())
                }
        }
    }
}
