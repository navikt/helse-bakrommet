package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.medBehandlingsperiode
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.saksbehandler
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.sikkerLogger
import java.time.OffsetDateTime
import java.util.*

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
                val inntektsforhold = call.receive<InntektsforholdCreateRequest>()
                sikkerLogger.info(
                    "Saksbehandler {} legger til inntektsforhold {} for periode {}",
                    call.saksbehandler().navIdent,
                    inntektsforhold,
                    periode,
                )

                val fraDatabasen =
                    inntektsforholdDao.opprettInntektsforhold(inntektsforhold.tilDatabaseType(periode.id))

                call.respondText(
                    fraDatabasen.tilDto().serialisertTilString(),
                    ContentType.Application.Json,
                    HttpStatusCode.Created,
                )
            }
        }

        put("/{inntektsforhold}") {
            put("/dagoversikt") {
            }
        }
    }
}

data class InntektsforholdCreateRequest(
    val kategorisering: JsonNode,
) {
    fun tilDatabaseType(behandlingsperiodeId: UUID) =
        Inntektsforhold(
            id = UUID.randomUUID(),
            kategorisering = kategorisering,
            kategoriseringGenerert = null,
            sykmeldtFraForholdet = kategorisering["ER_SYKMELDT"].asText() == "ER_SYKMELDT_JA",
            dagoversikt = "[]".asJsonNode(),
            dagoversiktGenerert = null,
            saksbehandlingsperiodeId = behandlingsperiodeId,
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = emptyList(),
        )
}
