package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.PARAM_YRKESAKTIVITETUUID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektService
import no.nav.helse.bakrommet.saksbehandlingsperiode.periodeReferanse
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.util.UUID

fun RoutingCall.yrkesaktivitetReferanse() =
    YrkesaktivitetReferanse(
        saksbehandlingsperiodeReferanse = periodeReferanse(),
        yrkesaktivitetUUID = parameters[PARAM_YRKESAKTIVITETUUID].somGyldigUUID(),
    )

data class YrkesaktivitetDTO(
    val id: UUID,
    val kategorisering: Map<String, String>,
    val dagoversikt: List<Dag>?,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder?,
    val inntektRequest: InntektRequest?,
    val inntektData: InntektData?,
)

fun YrkesaktivitetDbRecord.tilDto() =
    YrkesaktivitetDTO(
        id = id,
        kategorisering = kategorisering,
        dagoversikt = dagoversikt,
        generertFraDokumenter = generertFraDokumenter,
        perioder = perioder,
        inntektRequest = inntektRequest,
        inntektData = inntektData,
    )

internal fun Route.saksbehandlingsperiodeYrkesaktivitetRoute(
    service: YrkesaktivitetService,
    inntektservice: InntektService,
) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/yrkesaktivitet") {
        get {
            val yrkesaktiviteter = service.hentYrkesaktivitetFor(call.periodeReferanse())
            val yrkesaktivitetDto = yrkesaktiviteter.map { it.tilDto() }
            call.respondText(
                objectMapperCustomSerde.writeValueAsString(yrkesaktivitetDto),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        post {
            val yrkesaktivitetCreateRequest = call.receive<YrkesaktivitetCreateRequest>()

            // Valider og konverter til sealed class
            val validertKategorisering =
                YrkesaktivitetKategoriseringMapper.fromMap(yrkesaktivitetCreateRequest.kategorisering)

            val yrkesaktivitet =
                service.opprettYrkesaktivitet(
                    call.periodeReferanse(),
                    validertKategorisering,
                    call.saksbehandler(),
                )
            call.respondText(
                yrkesaktivitet.tilDto().serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.Created,
            )
        }

        route("/{$PARAM_YRKESAKTIVITETUUID}") {
            delete {
                service.slettYrkesaktivitet(call.yrkesaktivitetReferanse(), call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/dagoversikt") {
                val dagerSomSkalOppdateres = call.receive<DagerSomSkalOppdateres>()
                service.oppdaterDagoversiktDager(
                    call.yrkesaktivitetReferanse(),
                    dagerSomSkalOppdateres,
                    call.saksbehandler(),
                )
                call.respond(HttpStatusCode.NoContent)
            }
            put("/kategorisering") {
                val kategoriseringMap = call.receive<Map<String, String>>()

                // Valider og konverter til sealed class
                val validertKategorisering = YrkesaktivitetKategoriseringMapper.fromMap(kategoriseringMap)

                service.oppdaterKategorisering(
                    call.yrkesaktivitetReferanse(),
                    validertKategorisering,
                    call.saksbehandler(),
                )
                call.respond(HttpStatusCode.NoContent)
            }
            put("/perioder") {
                val perioderJson = call.receiveText()
                val perioder: Perioder? =
                    if (perioderJson == "null") null else objectMapper.readValue(perioderJson, Perioder::class.java)
                service.oppdaterPerioder(call.yrkesaktivitetReferanse(), perioder, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            route("/inntekt") {
                put {
                    val inntektRequest = call.receive<InntektRequest>()
                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse()

                    inntektservice.oppdaterInntekt(yrkesaktivitetRef, inntektRequest, call.saksbehandler())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

data class YrkesaktivitetCreateRequest(
    val kategorisering: Map<String, String>,
)
