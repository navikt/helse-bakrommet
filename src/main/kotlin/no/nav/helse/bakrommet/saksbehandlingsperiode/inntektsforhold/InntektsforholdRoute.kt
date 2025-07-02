package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.saksbehandlingsperiode.medBehandlingsperiode
import no.nav.helse.bakrommet.util.saksbehandler
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.sikkerLogger
import no.nav.helse.bakrommet.util.somGyldigUUID
import no.nav.helse.bakrommet.util.toJsonNode
import java.time.OffsetDateTime
import java.util.*

internal suspend inline fun ApplicationCall.medInntektsforhold(
    personDao: PersonDao,
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    inntektsforholdDao: InntektsforholdDao,
    crossinline block: suspend (inntektsforhold: Inntektsforhold) -> Unit,
) {
    this.medBehandlingsperiode(personDao, saksbehandlingsperiodeDao) { periode ->
        val inntektsforholdId = parameters["inntektsforholdUUID"].somGyldigUUID()
        val inntektsforhold =
            inntektsforholdDao.hentInntektsforhold(inntektsforholdId)
                ?: throw IkkeFunnetException("Inntektsforhold ikke funnet")
        require(inntektsforhold.saksbehandlingsperiodeId == periode.id) {
            "Inntektsforhold (id=$inntektsforholdId) tilhører ikke behandlingsperiode (id=${periode.id})"
        }
        block(inntektsforhold)
    }
}

internal fun Route.saksbehandlingsperiodeInntektsforholdRoute(
    saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    personDao: PersonDao,
    inntektsforholdDao: InntektsforholdDao,
) {
    route("/v1/{personId}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/inntektsforhold") {
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

                val dagoversikt =
                    if (inntektsforhold.kategorisering.skalHaDagoversikt()) {
                        initialiserDager(periode.fom, periode.tom)
                    } else {
                        null
                    }

                val fraDatabasen =
                    inntektsforholdDao.opprettInntektsforhold(inntektsforhold.tilDatabaseType(periode.id, dagoversikt))

                call.respondText(
                    fraDatabasen.tilDto().serialisertTilString(),
                    ContentType.Application.Json,
                    HttpStatusCode.Created,
                )
            }
        }

        route("/{inntektsforholdUUID}") {
            put("/dagoversikt") {
                val dagoversikt = call.receive<JsonNode>()
                call.medInntektsforhold(personDao, saksbehandlingsperiodeDao, inntektsforholdDao) { inntektsforhold ->
                    inntektsforholdDao.oppdaterDagoversikt(inntektsforhold, dagoversikt)
                }
                call.respond(HttpStatusCode.NoContent)
            }
            put("/kategorisering") {
                val kategorisering = call.receive<JsonNode>()
                call.medInntektsforhold(personDao, saksbehandlingsperiodeDao, inntektsforholdDao) { inntektsforhold ->
                    inntektsforholdDao.oppdaterKategorisering(inntektsforhold, kategorisering)
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

data class InntektsforholdCreateRequest(
    val kategorisering: JsonNode,
) {
    fun tilDatabaseType(
        behandlingsperiodeId: UUID,
        dagoversikt: List<Dag>?,
    ) = Inntektsforhold(
        id = UUID.randomUUID(),
        kategorisering = kategorisering,
        kategoriseringGenerert = null,
        dagoversikt = dagoversikt?.toJsonNode(),
        dagoversiktGenerert = null,
        saksbehandlingsperiodeId = behandlingsperiodeId,
        opprettet = OffsetDateTime.now(),
        generertFraDokumenter = emptyList(),
    )
}

fun JsonNode.skalHaDagoversikt(): Boolean {
    val erSykmeldt = this.get("ER_SYKMELDT")?.asText()
    return erSykmeldt == "ER_SYKMELDT_JA" || erSykmeldt == null
}
