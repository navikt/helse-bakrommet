package no.nav.helse.bakrommet.api.yrkesaktivitet

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.*
import no.nav.helse.bakrommet.api.auth.bruker
import no.nav.helse.bakrommet.api.auth.saksbehandlerOgToken
import no.nav.helse.bakrommet.api.behandling.hentOgVerifiserBehandling
import no.nav.helse.bakrommet.api.behandling.sjekkErÅpenOgTildeltSaksbehandler
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.InntektsmeldingMatcherService
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.hentAInntektForYrkesaktivitet
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.hentPensjonsgivendeInntektForYrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.maybeOrgnummer
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.Organisasjon
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
            db
                .transactional {
                    val behandling = this.hentOgVerifiserBehandling(call)
                    val yrkesaktiviteter = yrkesaktivitetRepository.finn(behandling.id)
                    val alleOrgnummer = yrkesaktiviteter.mapNotNull { it.kategorisering.maybeOrgnummer() }.toSet()

                    val organisasjonsnavn = organisasjonsnavnProvider.hentFlereOrganisasjonsnavn(alleOrgnummer)

                    yrkesaktiviteter.map { yrkesaktivitet ->
                        yrkesaktivitet.tilDto(organisasjonsnavn)
                    }
                }.let {
                    call.respondJson(it)
                }
        }

        post {
            val request = call.receive<YrkesaktivitetCreateRequestDto>()
            db
                .transactional {
                    val behandling = this.hentOgVerifiserBehandling(call).sjekkErÅpenOgTildeltSaksbehandler(call.bruker())

                    val kategorisering: YrkesaktivitetKategorisering = request.tilYrkesaktivitetKategorisering()

                    val dagoversikt =
                        if (kategorisering.sykmeldt) {
                            Dagoversikt.kunArbeidsdager(behandling.fom, behandling.tom)
                        } else {
                            null
                        }
                    val yrkesaktivitet =
                        Yrkesaktivitet
                            .opprett(
                                kategorisering = kategorisering,
                                kategoriseringGenerert = null,
                                dagoversikt = dagoversikt,
                                dagoversiktGenerert = null,
                                behandlingId = behandling.id,
                                generertFraDokumenter = emptyList(),
                            ).also {
                                yrkesaktivitetRepository.lagre(it)
                            }

                    val organisasjon =
                        kategorisering
                            .maybeOrgnummer()
                            ?.let { organisasjonsnavnProvider.hentOrganisasjonsnavn(it) }
                    yrkesaktivitet.tilDto(organisasjon)
                }.let {
                    call.respondJson(it, status = HttpStatusCode.Created)
                }
        }

        route("/{$PARAM_YRKESAKTIVITETUUID}") {
            delete {
                yrkesaktivitetService.slettYrkesaktivitet(
                    call.yrkesaktivitetReferanse(personService),
                    call.bruker(),
                )
                call.respond(HttpStatusCode.NoContent)
            }

            put("/dagoversikt") {
                val request = call.receive<DagerSomSkalOppdateresDto>()
                yrkesaktivitetService.oppdaterDagoversiktDager(
                    call.yrkesaktivitetReferanse(personService),
                    request.dager.map { it.tilDag() },
                    call.bruker(),
                )
                call.respond(HttpStatusCode.NoContent)
            }

            put("/kategorisering") {
                val kategoriseringRequest = call.receive<YrkesaktivitetKategoriseringDto>()
                db.transactional {
                    val yrkesaktivitet = this.hentOgVerifiserYrkesaktivitet(call)
                    val behandling = behandlingRepository.hent(yrkesaktivitet.behandlingId).sjekkErÅpenOgTildeltSaksbehandler(call.bruker())
                    // Validerer at organisasjon finnes hvis orgnummer er satt
                    val orgnummer = kategoriseringRequest.maybeOrgnummer()
                    if (orgnummer != null && !organisasjonsnavnProvider.eksisterer(orgnummer)) {
                        throw IkkeFunnetException(
                            title = "Organisasjon ikke funnet",
                            detail = "Fant ikke organisasjon i EREG for organisasjonsnummer $orgnummer",
                        )
                    }

                    val nyKategorisering = kategoriseringRequest.tilYrkesaktivitetKategorisering()
                    yrkesaktivitet.nyKategorisering(nyKategorisering)
                    yrkesaktivitetRepository.lagre(yrkesaktivitet)

                    // Hvis kategoriseringen endrer seg så må vi slette inntektdata og inntektrequest og beregning
                    // Slett sykepengegrunnlag og utbetalingsberegning når yrkesaktivitet endres
                    // Vi må alltid beregne på nytt når kategorisering endres
                    beregningDao.slettBeregning(yrkesaktivitet.behandlingId.value, failSilently = true)

                    // hvis sykepengegrunnlaget eies av denne perioden, slett det
                    behandling.let { behandling ->
                        behandling.sykepengegrunnlagId?.let { sykepengegrunnlagId ->
                            val spgRecord = sykepengegrunnlagDao.hentSykepengegrunnlag(sykepengegrunnlagId)
                            if (spgRecord.opprettetForBehandling == behandling.id.value) {
                                behandlingDao.oppdaterSykepengegrunnlagId(behandling.id.value, null)
                                sykepengegrunnlagDao.slettSykepengegrunnlag(sykepengegrunnlagId)
                            }
                        }
                    }
                }
                call.respond(HttpStatusCode.NoContent)
            }

            put("/perioder") {
                val perioderJson = call.receiveText()
                val perioder: PerioderDto? =
                    if (perioderJson == "null") null else objectMapper.readValue(perioderJson, PerioderDto::class.java)
                val perioderDomain = perioder?.tilPerioder()
                yrkesaktivitetService.oppdaterPerioder(
                    call.yrkesaktivitetReferanse(personService),
                    perioderDomain,
                    call.bruker(),
                )
                call.respond(HttpStatusCode.NoContent)
            }

            route("/inntekt") {
                put {
                    val inntektRequest = call.receive<InntektRequestDto>()
                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse(personService)
                    inntektservice.oppdaterInntekt(
                        yrkesaktivitetRef,
                        inntektRequest.tilInntektRequest(),
                        call.saksbehandlerOgToken(),
                    )
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            route("/refusjon") {
                put {
                    val refusjonBody = call.receiveText()
                    val refusjon: List<RefusjonsperiodeDto>? =
                        if (refusjonBody == "null") {
                            null
                        } else {
                            objectMapper.readValue(
                                refusjonBody,
                                objectMapper.typeFactory.constructCollectionType(
                                    List::class.java,
                                    RefusjonsperiodeDto::class.java,
                                ),
                            )
                        }
                    val refusjonDomain = refusjon?.map { it.tilRefusjonsperiode() }
                    val yrkesaktivitetRef = call.yrkesaktivitetReferanse(personService)
                    yrkesaktivitetService.oppdaterRefusjon(yrkesaktivitetRef, refusjonDomain, call.bruker())
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

private fun Yrkesaktivitet.tilDto(
    organisasjonsnavn: Map<String, Organisasjon>,
): YrkesaktivitetDto =
    tilDto(
        kategorisering
            .maybeOrgnummer()
            ?.let { organisasjonsnavn[it] },
    )

private fun Yrkesaktivitet.tilDto(
    organisasjon: Organisasjon?,
): YrkesaktivitetDto =
    YrkesaktivitetDto(
        id = id.value,
        kategorisering = kategorisering.tilYrkesaktivitetKategoriseringDto(),
        dagoversikt = dagoversikt?.tilMergetDagoversikt(),
        generertFraDokumenter = generertFraDokumenter.map { it },
        perioder = perioder?.tilPerioderDto(),
        inntektRequest = inntektRequest?.tilInntektRequestDto(),
        inntektData = inntektData?.tilInntektDataDto(),
        refusjon = refusjon?.map { it.tilRefusjonsperiodeDto() },
        orgnavn = organisasjon?.navn,
    )
