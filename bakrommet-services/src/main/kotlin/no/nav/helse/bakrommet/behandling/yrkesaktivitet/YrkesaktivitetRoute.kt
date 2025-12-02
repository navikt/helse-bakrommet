package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import com.fasterxml.jackson.module.kotlin.readValue
import hentPensjonsgivendeInntektForYrkesaktivitet
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.PARAM_YRKESAKTIVITETUUID
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.InntektsmeldingMatcherService
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.AInntektResponse
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.hentAInntektForYrkesaktivitet
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import no.nav.helse.bakrommet.serde.receiveWithCustomMapper
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
    val kategorisering: YrkesaktivitetKategorisering,
    val dagoversikt: List<Dag>?,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder?,
    val inntektRequest: InntektRequest?,
    val inntektData: InntektData?,
    val refusjon: List<Refusjonsperiode>? = null,
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
        refusjon = refusjon,
    )

internal fun Route.yrkesaktivitetRoute(
    yrkesaktivitetService: YrkesaktivitetService,
    inntektservice: InntektService,
    inntektsmeldingMatcherService: InntektsmeldingMatcherService,
    personService: PersonService,
) {
    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/yrkesaktivitet") {
        get {
            val yrkesaktiviteter = yrkesaktivitetService.hentYrkesaktivitetFor(call.periodeReferanse())
            val yrkesaktivitetDto = yrkesaktiviteter.map { it.tilDto() }
            call.respondText(
                objectMapperCustomSerde.writeValueAsString(yrkesaktivitetDto),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        post {
            val yrkesaktivitetCreateRequest = call.receive<YrkesaktivitetCreateRequest>()

            val yrkesaktivitet =
                yrkesaktivitetService.opprettYrkesaktivitet(
                    call.periodeReferanse(),
                    yrkesaktivitetCreateRequest.kategorisering,
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
                yrkesaktivitetService.slettYrkesaktivitet(call.yrkesaktivitetReferanse(), call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/dagoversikt") {
                val dagerSomSkalOppdateres = call.receive<DagerSomSkalOppdateres>()
                yrkesaktivitetService.oppdaterDagoversiktDager(
                    call.yrkesaktivitetReferanse(),
                    dagerSomSkalOppdateres,
                    call.saksbehandler(),
                )
                call.respond(HttpStatusCode.NoContent)
            }
            put("/kategorisering") {
                val kategorisering = call.receive<YrkesaktivitetKategorisering>()

                yrkesaktivitetService.oppdaterKategorisering(
                    call.yrkesaktivitetReferanse(),
                    kategorisering,
                    call.saksbehandler(),
                )
                call.respond(HttpStatusCode.NoContent)
            }
            put("/perioder") {
                val perioderJson = call.receiveText()
                val perioder: Perioder? =
                    if (perioderJson == "null") null else objectMapper.readValue(perioderJson, Perioder::class.java)
                yrkesaktivitetService.oppdaterPerioder(call.yrkesaktivitetReferanse(), perioder, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }
            route("/inntekt") {
                put {
                    val inntektRequest = call.receiveWithCustomMapper<InntektRequest>(objectMapperCustomSerde)
                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse()

                    inntektservice.oppdaterInntekt(yrkesaktivitetRef, inntektRequest, call.saksbehandlerOgToken())
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            route("/refusjon") {
                put {
                    val refusjonBody = call.receiveText()

                    val refusjon: List<Refusjonsperiode>? = objectMapperCustomSerde.readValue(refusjonBody)

                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse()

                    yrkesaktivitetService.oppdaterRefusjon(yrkesaktivitetRef, refusjon, call.saksbehandler())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            get("/inntektsmeldinger") {
                call.medIdent(personService) { fnr, personId ->
                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse()

                    val inntektsmeldinger =
                        inntektsmeldingMatcherService.hentInntektsmeldingerForYrkesaktivitet(
                            ref = yrkesaktivitetRef,
                            fnr = fnr,
                            saksbehandlerToken = call.request.bearerToken(),
                        )

                    call.respondText(
                        inntektsmeldinger.serialisertTilString(),
                        ContentType.Application.Json,
                        HttpStatusCode.OK,
                    )
                }
            }
            get("/pensjonsgivendeinntekt") {
                val yrkesaktivitetRef = call.yrkesaktivitetReferanse()
                val response =
                    inntektservice.hentPensjonsgivendeInntektForYrkesaktivitet(
                        ref = yrkesaktivitetRef,
                        saksbehandler = call.saksbehandlerOgToken(),
                    )

                val responseDto =
                    when (response) {
                        is PensjonsgivendeInntektResponse.Suksess ->
                            PensjonsgivendeInntektSuccessResponse(data = response.data)

                        is PensjonsgivendeInntektResponse.Feil ->
                            PensjonsgivendeInntektFeilResponse(feilmelding = response.feilmelding)
                    }

                call.respondText(
                    objectMapperCustomSerde.writeValueAsString(responseDto),
                    ContentType.Application.Json,
                    HttpStatusCode.OK,
                )
            }
            get("/ainntekt") {
                val yrkesaktivitetRef = call.yrkesaktivitetReferanse()
                val response =
                    inntektservice.hentAInntektForYrkesaktivitet(
                        ref = yrkesaktivitetRef,
                        saksbehandler = call.saksbehandlerOgToken(),
                    )

                val responseDto =
                    when (response) {
                        is AInntektResponse.Suksess ->
                            AinntektSuccessResponse(data = response.data)

                        is AInntektResponse.Feil ->
                            AinntektFeilResponse(feilmelding = response.feilmelding)
                    }

                call.respondText(
                    objectMapperCustomSerde.writeValueAsString(responseDto),
                    ContentType.Application.Json,
                    HttpStatusCode.OK,
                )
            }
        }
    }
}

data class YrkesaktivitetCreateRequest(
    val kategorisering: YrkesaktivitetKategorisering,
)

data class PensjonsgivendeInntektSuccessResponse(
    val success: Boolean = true,
    val data: InntektData,
)

data class PensjonsgivendeInntektFeilResponse(
    val success: Boolean = false,
    val feilmelding: String,
)

data class AinntektSuccessResponse(
    val success: Boolean = true,
    val data: InntektData,
)

data class AinntektFeilResponse(
    val success: Boolean = false,
    val feilmelding: String,
)
