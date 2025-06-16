package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.medBehandlingsperiode
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.saksbehandlingsperiodeInntektsforholdRoute(
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    personDao: PersonDao,
    inntektsforholdDao: InntektsforholdDao,
) {
    route("/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/inntektsforhold") {
        get {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
                val inntektsforholdFraDB = inntektsforholdDao.hentInntektsforholdFor(periode)
                call.respondText(
                    inntektsforholdFraDB.map { it.tilDto() }.serialisertTilString(),
                    ContentType.Application.Json,
                    HttpStatusCode.OK,
                )
            }
        }

        post {
            call.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
            }
        }
    }
}
