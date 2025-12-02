package no.nav.helse.bakrommet.api.yrkesaktivitet

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.PARAM_YRKESAKTIVITETUUID
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.auth.bearerToken
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.InntektsmeldingMatcherService
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.hentAInntektForYrkesaktivitet
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.hentPensjonsgivendeInntektForYrkesaktivitet
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.person.medIdent
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.somGyldigUUID

fun RoutingCall.yrkesaktivitetReferanse() =
    YrkesaktivitetReferanse(
        saksbehandlingsperiodeReferanse = periodeReferanse(),
        yrkesaktivitetUUID = parameters[PARAM_YRKESAKTIVITETUUID].somGyldigUUID(),
    )

fun Route.yrkesaktivitetRoute(
    yrkesaktivitetService: YrkesaktivitetService,
    inntektservice: InntektService,
    inntektsmeldingMatcherService: InntektsmeldingMatcherService,
    personService: PersonService,
) {
    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/yrkesaktivitet") {
        get {
            val yrkesaktiviteter = yrkesaktivitetService.hentYrkesaktivitetFor(call.periodeReferanse())
            val yrkesaktivitetDto = yrkesaktiviteter.map { it.tilYrkesaktivitetDto() }
            call.respondJson(yrkesaktivitetDto)
        }

        post {
            val request = call.receive<YrkesaktivitetCreateRequestDto>()
            val yrkesaktivitet =
                yrkesaktivitetService.opprettYrkesaktivitet(
                    call.periodeReferanse(),
                    request.tilYrkesaktivitetKategorisering(),
                    call.saksbehandler(),
                )
            call.respondJson(yrkesaktivitet.tilYrkesaktivitetDto(), status = HttpStatusCode.Created)
        }

        route("/{$PARAM_YRKESAKTIVITETUUID}") {
            delete {
                yrkesaktivitetService.slettYrkesaktivitet(call.yrkesaktivitetReferanse(), call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }

            put("/dagoversikt") {
                val request = call.receive<DagerSomSkalOppdateresDto>()
                yrkesaktivitetService.oppdaterDagoversiktDager(
                    call.yrkesaktivitetReferanse(),
                    request.dager.tilJsonNode(),
                    call.saksbehandler(),
                )
                call.respond(HttpStatusCode.NoContent)
            }

            put("/kategorisering") {
                val kategorisering = call.receive<YrkesaktivitetKategoriseringDto>()
                yrkesaktivitetService.oppdaterKategorisering(
                    call.yrkesaktivitetReferanse(),
                    kategorisering.tilYrkesaktivitetKategorisering(),
                    call.saksbehandler(),
                )
                call.respond(HttpStatusCode.NoContent)
            }

            put("/perioder") {
                val perioderJson = call.receiveText()
                val perioder: PerioderDto? = if (perioderJson == "null") null else objectMapper.readValue(perioderJson, PerioderDto::class.java)
                val perioderDomain = perioder?.tilPerioder()
                yrkesaktivitetService.oppdaterPerioder(call.yrkesaktivitetReferanse(), perioderDomain, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }

            route("/inntekt") {
                put {
                    val inntektRequest = call.receive<InntektRequestDto>()
                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse()
                    inntektservice.oppdaterInntekt(yrkesaktivitetRef, inntektRequest.tilInntektRequest(), call.saksbehandlerOgToken())
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            route("/refusjon") {
                put {
                    val refusjonBody = call.receiveText()
                    val refusjon: List<RefusjonsperiodeDto>? = if (refusjonBody == "null") null else objectMapper.readValue(refusjonBody, objectMapper.typeFactory.constructCollectionType(List::class.java, RefusjonsperiodeDto::class.java))
                    val refusjonDomain = refusjon?.map { it.tilRefusjonsperiode() }
                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse()
                    yrkesaktivitetService.oppdaterRefusjon(yrkesaktivitetRef, refusjonDomain, call.saksbehandler())
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
                call.respondJson(response.tilPensjonsgivendeInntektResponseDto())
            }

            get("/ainntekt") {
                val yrkesaktivitetRef = call.yrkesaktivitetReferanse()
                val response =
                    inntektservice.hentAInntektForYrkesaktivitet(
                        ref = yrkesaktivitetRef,
                        saksbehandler = call.saksbehandlerOgToken(),
                    )
                call.respondJson(response.tilAinntektResponseDto())
            }
        }
    }
}
