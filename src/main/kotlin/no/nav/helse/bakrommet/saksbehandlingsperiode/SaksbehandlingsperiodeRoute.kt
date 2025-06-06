package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.util.saksbehandler
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

internal fun Route.saksbehandlingsperiodeRoute(
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    personDao: PersonDao,
    dokumentHenter: DokumentHenter,
) = route("/v1/{personId}/saksbehandlingsperioder") {
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
            if (body.søknader != null && body.søknader.isNotEmpty()) {
                dokumentHenter.hentOgLagreSøknaderOgInntekter(nyPeriode.id, body.søknader, call.request.bearerToken())
            }
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
