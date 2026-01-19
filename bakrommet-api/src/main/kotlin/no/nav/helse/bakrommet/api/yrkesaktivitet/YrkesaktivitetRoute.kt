package no.nav.helse.bakrommet.api.yrkesaktivitet

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.*
import no.nav.helse.bakrommet.api.auth.saksbehandler
import no.nav.helse.bakrommet.api.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.api.behandling.hentOgVerifiserBehandling
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.InntektsmeldingMatcherService
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.hentAInntektForYrkesaktivitet
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.hentPensjonsgivendeInntektForYrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.maybeOrgnummer
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.OrganisasjonsnavnProvider
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString

fun Route.yrkesaktivitetRoute(
    yrkesaktivitetService: YrkesaktivitetService,
    inntektservice: InntektService,
    inntektsmeldingMatcherService: InntektsmeldingMatcherService,
    personService: PersonService,
    organisasjonsnavnProvider: OrganisasjonsnavnProvider,
    db: DbDaoer<AlleDaoer>,
) {
    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/yrkesaktivitet") {
        get {
            db.transactional {
                val behandling = this.hentOgVerifiserBehandling(call)
                val yrkesaktiviteter = yrkesaktivitetRepository.finn(behandling.id)
                val alleOrgnummer = yrkesaktiviteter.mapNotNull { it.kategorisering.maybeOrgnummer() }.toSet()

                val organisasjonsnavn = organisasjonsnavnProvider.hentFlereOrganisasjonsnavn(alleOrgnummer)


                yrkesaktiviteter.map { yrkesaktivitet ->
                    YrkesaktivitetDto(
                        id = yrkesaktivitet.id.value,
                        kategorisering = yrkesaktivitet.kategorisering.tilYrkesaktivitetKategoriseringDto(),
                        dagoversikt = yrkesaktivitet.dagoversikt?.tilMergetDagoversikt(),
                        generertFraDokumenter = yrkesaktivitet.generertFraDokumenter.map { it },
                        perioder = yrkesaktivitet.perioder?.tilPerioderDto(),
                        inntektRequest = yrkesaktivitet.inntektRequest?.tilInntektRequestDto(),
                        inntektData = yrkesaktivitet.inntektData?.tilInntektDataDto(),
                        refusjon = yrkesaktivitet.refusjon?.map { it.tilRefusjonsperiodeDto() },

                        orgnavn =
                            yrkesaktivitet.kategorisering
                                .maybeOrgnummer()
                                ?.let { organisasjonsnavn[it]?.navn },
                    )
                }
            }.let {
                call.respondJson(it)
            }

        }

        post {
            val request = call.receive<YrkesaktivitetCreateRequestDto>()
            val yrkesaktivitet =
                yrkesaktivitetService.opprettYrkesaktivitet(
                    call.periodeReferanse(personService),
                    request.tilYrkesaktivitetKategorisering(),
                    call.saksbehandler(),
                )
            call.respondJson(yrkesaktivitet.tilYrkesaktivitetDto(), status = HttpStatusCode.Created)
        }

        route("/{$PARAM_YRKESAKTIVITETUUID}") {
            delete {
                yrkesaktivitetService.slettYrkesaktivitet(call.yrkesaktivitetReferanse(personService), call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }

            put("/dagoversikt") {
                val request = call.receive<DagerSomSkalOppdateresDto>()
                yrkesaktivitetService.oppdaterDagoversiktDager(
                    call.yrkesaktivitetReferanse(personService),
                    request.dager.map { it.tilDag() },
                    call.saksbehandler(),
                )
                call.respond(HttpStatusCode.NoContent)
            }

            put("/kategorisering") {
                val kategorisering = call.receive<YrkesaktivitetKategoriseringDto>()
                yrkesaktivitetService.oppdaterKategorisering(
                    call.yrkesaktivitetReferanse(personService),
                    kategorisering.tilYrkesaktivitetKategorisering(),
                    call.saksbehandler(),
                )
                call.respond(HttpStatusCode.NoContent)
            }

            put("/perioder") {
                val perioderJson = call.receiveText()
                val perioder: PerioderDto? = if (perioderJson == "null") null else objectMapper.readValue(perioderJson, PerioderDto::class.java)
                val perioderDomain = perioder?.tilPerioder()
                yrkesaktivitetService.oppdaterPerioder(call.yrkesaktivitetReferanse(personService), perioderDomain, call.saksbehandler())
                call.respond(HttpStatusCode.NoContent)
            }

            route("/inntekt") {
                put {
                    val inntektRequest = call.receive<InntektRequestDto>()
                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse(personService)
                    inntektservice.oppdaterInntekt(yrkesaktivitetRef, inntektRequest.tilInntektRequest(), call.saksbehandlerOgToken())
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            route("/refusjon") {
                put {
                    val refusjonBody = call.receiveText()
                    val refusjon: List<RefusjonsperiodeDto>? = if (refusjonBody == "null") null else objectMapper.readValue(refusjonBody, objectMapper.typeFactory.constructCollectionType(List::class.java, RefusjonsperiodeDto::class.java))
                    val refusjonDomain = refusjon?.map { it.tilRefusjonsperiode() }
                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse(personService)
                    yrkesaktivitetService.oppdaterRefusjon(yrkesaktivitetRef, refusjonDomain, call.saksbehandler())
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            get("/inntektsmeldinger") {
                val naturligIdent = call.naturligIdent(personService)

                val yrkesaktivitetRef = call.yrkesaktivitetReferanse(personService)
                val inntektsmeldinger =
                    inntektsmeldingMatcherService.hentInntektsmeldingerForYrkesaktivitet(
                        ref = yrkesaktivitetRef,
                        fnr = naturligIdent.value,
                        bruker = call.saksbehandlerOgToken(),
                    )
                call.respondText(
                    inntektsmeldinger.serialisertTilString(),
                    ContentType.Application.Json,
                    HttpStatusCode.OK,
                )
            }

            get("/pensjonsgivendeinntekt") {
                val yrkesaktivitetRef = call.yrkesaktivitetReferanse(personService)
                val response =
                    inntektservice.hentPensjonsgivendeInntektForYrkesaktivitet(
                        ref = yrkesaktivitetRef,
                        saksbehandler = call.saksbehandlerOgToken(),
                    )
                call.respondJson(response.tilPensjonsgivendeInntektResponseDto())
            }

            get("/ainntekt") {
                val yrkesaktivitetRef = call.yrkesaktivitetReferanse(personService)
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
