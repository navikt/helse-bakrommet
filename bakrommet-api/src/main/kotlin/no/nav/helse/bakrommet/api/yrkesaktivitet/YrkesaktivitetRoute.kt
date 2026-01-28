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
import no.nav.helse.bakrommet.behandling.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.behandling.beregning.beregnUtbetaling
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.somInntektsmeldingObjektListe
import no.nav.helse.bakrommet.behandling.inntekter.InntektService
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.hentAInntektForYrkesaktivitet
import no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse.henting.hentPensjonsgivendeInntektForYrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.Dag
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.Dagtype
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.maybeOrgnummer
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.InntektsmeldingProvider
import no.nav.helse.bakrommet.infrastruktur.provider.Organisasjon
import no.nav.helse.bakrommet.infrastruktur.provider.OrganisasjonsnavnProvider
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString

fun Route.yrkesaktivitetRoute(
    inntektservice: InntektService,
    inntektsmeldingProvider: InntektsmeldingProvider,
    organisasjonsnavnProvider: OrganisasjonsnavnProvider,
    pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
    inntektProvider: InntekterProvider,
    db: DbDaoer<AlleDaoer>,
) {
    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/yrkesaktivitet") {
        get {
            db
                .transactional {
                    val behandling = this.hentOgVerifiserBehandling(call)
                    val yrkesaktivitetsperioder = yrkesaktivitetsperiodeRepository.finn(behandling.id)
                    val alleOrgnummer = yrkesaktivitetsperioder.mapNotNull { it.kategorisering.maybeOrgnummer() }.toSet()

                    val organisasjonsnavn = organisasjonsnavnProvider.hentFlereOrganisasjonsnavn(alleOrgnummer)

                    yrkesaktivitetsperioder.map { yrkesaktivitetsperiode ->
                        yrkesaktivitetsperiode.tilDto(organisasjonsnavn)
                    }
                }.let {
                    call.respondJson(it)
                }
        }

        post {
            val request = call.receive<YrkesaktivitetCreateRequestDto>()
            db
                .transactional {
                    val behandling =
                        this.hentOgVerifiserBehandling(call).sjekkErÅpenOgTildeltSaksbehandler(call.bruker())

                    val kategorisering: YrkesaktivitetKategorisering = request.tilYrkesaktivitetKategorisering()

                    val dagoversikt =
                        if (kategorisering.sykmeldt) {
                            Dagoversikt.kunArbeidsdager(behandling.fom, behandling.tom)
                        } else {
                            null
                        }
                    val yrkesaktivitetsperiode =
                        Yrkesaktivitetsperiode
                            .opprett(
                                kategorisering = kategorisering,
                                kategoriseringGenerert = null,
                                dagoversikt = dagoversikt,
                                dagoversiktGenerert = null,
                                behandlingId = behandling.id,
                                generertFraDokumenter = emptyList(),
                            ).also {
                                yrkesaktivitetsperiodeRepository.lagre(it)
                            }

                    val organisasjon =
                        kategorisering
                            .maybeOrgnummer()
                            ?.let { organisasjonsnavnProvider.hentOrganisasjonsnavn(it) }
                    yrkesaktivitetsperiode.tilDto(organisasjon)
                }.let {
                    call.respondJson(it, status = HttpStatusCode.Created)
                }
        }

        route("/{$PARAM_YRKESAKTIVITETUUID}") {
            delete {
                db.transactional {
                    val bruker = call.bruker()
                    val behandling =
                        this
                            .hentOgVerifiserBehandling(call)
                            .sjekkErÅpenOgTildeltSaksbehandler(bruker)
                    val yrkesaktivitetsperiode =
                        yrkesaktivitetsperiodeRepository
                            .finn(call.yrkesaktivitetsperiodeId())
                            ?: return@transactional
                    if (!yrkesaktivitetsperiode.tilhører(behandling)) {
                        error("Yrkesaktivitet tilhører ikke behandling ${behandling.id.value}")
                    }
                    yrkesaktivitetsperiodeRepository.slett(yrkesaktivitetsperiode.id)
                    beregnSykepengegrunnlagOgUtbetaling(
                        behandling = behandling,
                        saksbehandler = bruker,
                    )
                }

                call.respond(HttpStatusCode.NoContent)
            }

            put("/dagoversikt") {
                val request = call.receive<DagerSomSkalOppdateresDto>()

                db.transactional {
                    val yrkesaktivitetsperiode = this.hentOgVerifiserYrkesaktivitetsperiode(call)
                    val saksbehandler = call.bruker()
                    val behandling =
                        behandlingRepository
                            .hent(yrkesaktivitetsperiode.behandlingId)
                            .sjekkErÅpenOgTildeltSaksbehandler(saksbehandler)

                    val dager = request.dager.map { it.tilDag() }.also { it.validerAvslagsgrunn() }
                    yrkesaktivitetsperiode.oppdaterDagoversikt(dager)
                    yrkesaktivitetsperiodeRepository.lagre(yrkesaktivitetsperiode)

                    beregnUtbetaling(
                        behandling = behandling,
                        saksbehandler = saksbehandler,
                    )
                }

                call.respond(HttpStatusCode.NoContent)
            }

            put("/kategorisering") {
                val kategoriseringRequest = call.receive<YrkesaktivitetKategoriseringDto>()
                db.transactional {
                    val yrkesaktivitetsperiode = this.hentOgVerifiserYrkesaktivitetsperiode(call)
                    val behandling =
                        behandlingRepository
                            .hent(yrkesaktivitetsperiode.behandlingId)
                            .sjekkErÅpenOgTildeltSaksbehandler(call.bruker())
                    // Validerer at organisasjon finnes hvis orgnummer er satt
                    val orgnummer = kategoriseringRequest.maybeOrgnummer()
                    if (orgnummer != null && !organisasjonsnavnProvider.eksisterer(orgnummer)) {
                        throw IkkeFunnetException(
                            title = "Organisasjon ikke funnet",
                            detail = "Fant ikke organisasjon i EREG for organisasjonsnummer $orgnummer",
                        )
                    }

                    val nyKategorisering = kategoriseringRequest.tilYrkesaktivitetKategorisering()
                    yrkesaktivitetsperiode.nyKategorisering(nyKategorisering)
                    yrkesaktivitetsperiodeRepository.lagre(yrkesaktivitetsperiode)

                    // Hvis kategoriseringen endrer seg så må vi slette inntektdata og inntektrequest og beregning
                    // Slett sykepengegrunnlag og utbetalingsberegning når yrkesaktivitet endres
                    // Vi må alltid beregne på nytt når kategorisering endres
                    beregningDao.slettBeregning(yrkesaktivitetsperiode.behandlingId.value, failSilently = true)

                    // hvis sykepengegrunnlaget eies av denne perioden, slett det
                    behandling.let { behandling ->
                        behandling.sykepengegrunnlagId?.let { sykepengegrunnlagId ->
                            val spgRecord = sykepengegrunnlagDao.hentSykepengegrunnlag(sykepengegrunnlagId.value)
                            if (spgRecord.opprettetForBehandling == behandling.id.value) {
                                behandlingDao.oppdaterSykepengegrunnlagId(behandling.id.value, null)
                                sykepengegrunnlagDao.slettSykepengegrunnlag(sykepengegrunnlagId.value)
                            }
                        }
                    }
                }
                call.respond(HttpStatusCode.NoContent)
            }

            put("/perioder") {
                val perioderJson = call.receiveText()
                val perioderDto: PerioderDto? =
                    if (perioderJson == "null") null else objectMapper.readValue(perioderJson, PerioderDto::class.java)
                val perioder = perioderDto?.tilPerioder()

                db.transactional {
                    val yrkesaktivitetsperiode = this.hentOgVerifiserYrkesaktivitetsperiode(call)
                    val saksbehandler = call.bruker()
                    val behandling =
                        behandlingRepository
                            .hent(yrkesaktivitetsperiode.behandlingId)
                            .sjekkErÅpenOgTildeltSaksbehandler(saksbehandler)

                    yrkesaktivitetsperiode.oppdaterPerioder(perioder)
                    yrkesaktivitetsperiodeRepository.lagre(yrkesaktivitetsperiode)

                    beregnUtbetaling(
                        behandling = behandling,
                        saksbehandler = saksbehandler,
                    )
                }

                call.respond(HttpStatusCode.NoContent)
            }

            route("/inntekt") {
                put {
                    val inntektRequest = call.receive<InntektRequestDto>()

                    db.transactional {
                        val yrkesaktivitetsperiode = this.hentOgVerifiserYrkesaktivitetsperiode(call)
                        val saksbehandler = call.bruker()
                        val behandling =
                            behandlingRepository
                                .hent(yrkesaktivitetsperiode.behandlingId)
                                .sjekkErÅpenOgTildeltSaksbehandler(saksbehandler)

                        inntektservice.oppdaterInntekt(
                            db = this,
                            yrkesaktivitetsperiode = yrkesaktivitetsperiode,
                            behandling = behandling,
                            request = inntektRequest.tilInntektRequest(),
                            saksbehandler = call.saksbehandlerOgToken(),
                        )
                    }

                    call.respond(HttpStatusCode.NoContent)
                }
            }

            route("/refusjon") {
                put {
                    val refusjonBody = call.receiveText()
                    val refusjonDto: List<RefusjonsperiodeDto>? =
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
                    val refusjon = refusjonDto?.map { it.tilRefusjonsperiode() }

                    db.transactional {
                        val yrkesaktivitetsperiode = this.hentOgVerifiserYrkesaktivitetsperiode(call)
                        val saksbehandler = call.bruker()
                        val behandling =
                            behandlingRepository
                                .hent(yrkesaktivitetsperiode.behandlingId)
                                .sjekkErÅpenOgTildeltSaksbehandler(saksbehandler)

                        yrkesaktivitetsperiode.oppdaterRefusjon(refusjon)
                        yrkesaktivitetsperiodeRepository.lagre(yrkesaktivitetsperiode)

                        beregnUtbetaling(
                            behandling = behandling,
                            saksbehandler = saksbehandler,
                        )
                    }

                    call.respond(HttpStatusCode.NoContent)
                }
            }

            get("/inntektsmeldinger") {
                val brukerOgToken = call.saksbehandlerOgToken()
                val (behandling, yrkesaktivitetsperiode) =
                    db.transactional {
                        val yrkesaktivitetsperiode = hentOgVerifiserYrkesaktivitetsperiode(call)
                        val behandling =
                            hentOgVerifiserBehandling(call)
                                .sjekkErÅpenOgTildeltSaksbehandler(brukerOgToken.bruker)

                        behandling to yrkesaktivitetsperiode
                    }

                if (yrkesaktivitetsperiode.kategorisering !is YrkesaktivitetKategorisering.Arbeidstaker) {
                    error("Kategorisering er ikke Arbeidstaker, da henter vi ikke inntektsmeldinger")
                }

                val inntektsmeldinger =
                    inntektsmeldingProvider
                        .hentInntektsmeldinger(
                            fnr = behandling.naturligIdent.value,
                            fom = null,
                            tom = null,
                            saksbehandlerToken = brukerOgToken.token,
                        ).somInntektsmeldingObjektListe()
                        .filter { it.virksomhetsnummer == yrkesaktivitetsperiode.kategorisering.maybeOrgnummer() }
                        .filter { it.foersteFravaersdag != null } // må ha fraværsdag for å matche
                        .filter { it.foersteFravaersdag!!.isAfter(behandling.skjæringstidspunkt.minusWeeks(4)) } // må ha fraværsdag for å matche
                        .filter { it.foersteFravaersdag!!.isBefore(behandling.skjæringstidspunkt.plusWeeks(4)) }
                call.respondText(
                    inntektsmeldinger.serialisertTilString(),
                    ContentType.Application.Json,
                    HttpStatusCode.OK,
                )
            }

            get("/pensjonsgivendeinntekt") {
                db
                    .transactional {
                        val yrkesaktivitetsperiode = this.hentOgVerifiserYrkesaktivitetsperiode(call)
                        val saksbehandler = call.bruker()
                        val behandling =
                            behandlingRepository
                                .hent(yrkesaktivitetsperiode.behandlingId)
                                .sjekkErÅpenOgTildeltSaksbehandler(saksbehandler)

                        hentPensjonsgivendeInntektForYrkesaktivitet(
                            saksbehandler = call.saksbehandlerOgToken(),
                            yrkesaktivitetsperiode = yrkesaktivitetsperiode,
                            behandling = behandling,
                            pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
                        ).tilPensjonsgivendeInntektResponseDto()
                    }.let { call.respondJson(it) }
            }

            get("/ainntekt") {
                db
                    .transactional {
                        val yrkesaktivitetsperiode = this.hentOgVerifiserYrkesaktivitetsperiode(call)
                        val saksbehandler = call.bruker()
                        val behandling =
                            behandlingRepository
                                .hent(yrkesaktivitetsperiode.behandlingId)
                                .sjekkErÅpenOgTildeltSaksbehandler(saksbehandler)

                        hentAInntektForYrkesaktivitet(
                            saksbehandler = call.saksbehandlerOgToken(),
                            yrkesaktivitetsperiode = yrkesaktivitetsperiode,
                            behandling = behandling,
                            inntekterProvider = inntektProvider,
                        ).tilAinntektResponseDto()
                    }.let { call.respondJson(it) }
            }
        }
    }
}

private fun List<Dag>.validerAvslagsgrunn() {
    this.forEach { dag ->
        if (dag.dagtype == Dagtype.Avslått && (dag.avslåttBegrunnelse ?: emptyList()).isEmpty()) {
            throw InputValideringException("Avslåtte dager må ha avslagsgrunn")
        }
    }
}

private fun Yrkesaktivitetsperiode.tilDto(
    organisasjonsnavn: Map<String, Organisasjon>,
): YrkesaktivitetDto =
    tilDto(
        kategorisering
            .maybeOrgnummer()
            ?.let { organisasjonsnavn[it] },
    )

private fun Yrkesaktivitetsperiode.tilDto(
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
