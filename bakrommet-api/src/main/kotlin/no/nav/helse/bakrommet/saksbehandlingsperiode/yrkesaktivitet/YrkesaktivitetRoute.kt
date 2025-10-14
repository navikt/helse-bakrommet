package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_INNTEKTSFORHOLDUUID
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.periodeReferanse
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.util.UUID

fun RoutingCall.inntektsforholdReferanse() =
    YrkesaktivitetReferanse(
        saksbehandlingsperiodeReferanse = periodeReferanse(),
        inntektsforholdUUID = parameters[PARAM_INNTEKTSFORHOLDUUID].somGyldigUUID(),
    )

data class YrkesaktivitetDTO(
    val id: UUID,
    val kategorisering: Map<String, String>,
    val dagoversikt: List<Dag>?,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder?,
)

fun Yrkesaktivitet.tilDto() =
    YrkesaktivitetDTO(
        id = id,
        kategorisering = kategorisering,
        dagoversikt = dagoversikt,
        generertFraDokumenter = generertFraDokumenter,
        perioder = perioder,
    )

internal fun Route.saksbehandlingsperiodeYrkesaktivitetRoute(service: YrkesaktivitetService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/yrkesaktivitet") {
        get {
            val inntektsforhold = service.hentYrkesaktivitetFor(call.periodeReferanse())
            call.respondText(
                inntektsforhold.map { it.tilDto() }.serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        post {
            val inntektsforholdRequest = call.receive<YrkesaktivitetCreateRequest>()

            // Valider og konverter til sealed class
            val validertKategorisering = YrkesaktivitetKategoriseringMapper.fromMap(inntektsforholdRequest.kategorisering)

            val inntektsforhold =
                service.opprettYrkesaktivitet(
                    call.periodeReferanse(),
                    validertKategorisering,
                    call.saksbehandler(),
                )
            call.respondText(
                inntektsforhold.tilDto().serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.Created,
            )
        }

        route("/{$PARAM_INNTEKTSFORHOLDUUID}") {
            delete {
                service.slettYrkesaktivitet(call.inntektsforholdReferanse(), call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/dagoversikt") {
                val dagerSomSkalOppdateres = call.receive<DagerSomSkalOppdateres>()
                service.oppdaterDagoversiktDager(call.inntektsforholdReferanse(), dagerSomSkalOppdateres, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/kategorisering") {
                val kategoriseringMap = call.receive<Map<String, String>>()

                // Valider og konverter til sealed class
                val validertKategorisering = YrkesaktivitetKategoriseringMapper.fromMap(kategoriseringMap)

                service.oppdaterKategorisering(call.inntektsforholdReferanse(), validertKategorisering, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/perioder") {
                val perioderJson = call.receiveText()
                val perioder: Perioder? = if (perioderJson == "null") null else objectMapper.readValue(perioderJson, Perioder::class.java)
                service.oppdaterPerioder(call.inntektsforholdReferanse(), perioder, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/inntekt") {
                val inntektRequestJson = call.receiveText()
                val inntektRequest = inntektRequestJson.deserializeToInntektRequest()

                service.oppdaterInntekt(call.inntektsforholdReferanse(), inntektRequest, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

data class YrkesaktivitetCreateRequest(
    val kategorisering: Map<String, String>,
)
