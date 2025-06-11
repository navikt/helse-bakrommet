package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.SaksbehandlingsperiodeIkkeFunnetException
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.somGyldigUUID
import no.nav.helse.bakrommet.util.saksbehandler
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

internal suspend inline fun ApplicationCall.medBehandlingsperiode(
    personDao: PersonDao,
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    crossinline block: suspend (saksbehandlingsperiode: Saksbehandlingsperiode) -> Unit,
) {
    this.medIdent(personDao) { fnr, spilleromPersonId ->
        val periodeId = parameters["periodeUUID"]!!.somGyldigUUID()
        val periode =
            saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(periodeId)
                ?: throw SaksbehandlingsperiodeIkkeFunnetException()
        if (periode.spilleromPersonId != spilleromPersonId) {
            throw InputValideringException("Ugyldig saksbehandlingsperiode")
        }
        block(periode)
    }
}

internal fun Route.saksbehandlingsperiodeRoute(
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    personDao: PersonDao,
    dokumentHenter: DokumentHenter,
    dokumentDao: DokumentDao,
) {
    route("/v1/{personId}/saksbehandlingsperioder") {
        data class CreatePeriodeRequest(
            val fom: String,
            val tom: String,
            val søknader: List<UUID>? = null,
        )

        /** Opprett en ny periode */
        post {
            call.medIdent(personDao) { fnr, spilleromPersonId ->
                val body = call.receive<CreatePeriodeRequest>()
                val saksbehandler = call.saksbehandler()
                val nyPeriode =
                    Saksbehandlingsperiode(
                        id = UUID.randomUUID(),
                        spilleromPersonId = spilleromPersonId,
                        opprettet = OffsetDateTime.now(),
                        opprettetAvNavIdent = saksbehandler.navIdent,
                        opprettetAvNavn = saksbehandler.navn,
                        fom = LocalDate.parse(body.fom),
                        tom = LocalDate.parse(body.tom),
                    )
                saksbehandlingsperiodeDao.opprettPeriode(nyPeriode)
                val innhentedeDokumenter =
                    if (body.søknader != null && body.søknader.isNotEmpty()) {
                        dokumentHenter.hentOgLagreSøknaderOgInntekter(
                            nyPeriode.id,
                            body.søknader,
                            call.request.bearerToken(),
                        )
                    } else {
                        emptyList()
                    }

                // TODO: Returner også innhentede dokumenter?

                call.respondText(nyPeriode.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.Created)
            }
        }

        /** Hent alle perioder for en person */
        get {
            call.medIdent(personDao) { fnr, spilleromPersonId ->
                val perioder = saksbehandlingsperiodeDao.finnPerioderForPerson(spilleromPersonId)
                call.respondText(perioder.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{personId}/saksbehandlingsperioder/{periodeUUID}") {
        get {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                call.respondText(periode.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }

    route("/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/dokumenter") {
        get {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val dokumenter = dokumentDao.hentDokumenterFor(periode.id)
                call.respondText(dokumenter.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }
}
