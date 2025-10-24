package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.ainntekt.Inntektoppslag
import no.nav.helse.bakrommet.ainntekt.tilInntektApiUt
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.*
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Inntektskategori
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.YearMonth
import kotlin.math.abs

interface InntektServiceDaoer :
    Beregningsdaoer,
    DokumentInnhentingDaoer {
    override val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    override val yrkesaktivitetDao: YrkesaktivitetDao
    override val sykepengegrunnlagDao: SykepengegrunnlagDao
    override val beregningDao: UtbetalingsberegningDao
    override val personDao: PersonDao
    override val dokumentDao: DokumentDao
}

class InntektService(
    daoer: InntektServiceDaoer,
    val inntektsmeldingClient: InntektsmeldingClient,
    val sigrunClient: SigrunClient,
    val aInntektClient: AInntektClient,
    sessionFactory: TransactionalSessionFactory<InntektServiceDaoer>,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    fun oppdaterInntekt(
        ref: YrkesaktivitetReferanse,
        request: InntektRequest,
        saksbehandler: BrukerOgToken,
    ) {
        db.transactional {
            val periode =
                saksbehandlingsperiodeDao.hentPeriode(
                    ref = ref.saksbehandlingsperiodeReferanse,
                    krav = saksbehandler.bruker.erSaksbehandlerPåSaken(),
                )
            if (periode.skjæringstidspunkt == null) {
                throw IllegalStateException("Kan ikke oppdatere inntekt før skjæringstidspunkt er satt på saksbehandlingsperiode (id=${periode.id})")
            }
            val yrkesaktivitet =
                yrkesaktivitetDao.hentYrkesaktivitet(ref.yrkesaktivitetUUID)
                    ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")
            require(yrkesaktivitet.saksbehandlingsperiodeId == periode.id) {
                "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
            }

            fun validerInntektskategori(forventet: Inntektskategori) {
                if (yrkesaktivitet.kategorisering.inntektskategori != forventet) {
                    throw IllegalStateException("Kan kun oppdatere ${forventet.name} inntekt på yrkesaktivitet med inntektskategori ${forventet.name}")
                }
            }

            when (request) {
                is InntektRequest.Arbeidstaker -> validerInntektskategori(Inntektskategori.ARBEIDSTAKER)
                is InntektRequest.SelvstendigNæringsdrivende -> validerInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
                is InntektRequest.Frilanser -> validerInntektskategori(Inntektskategori.FRILANSER)
                is InntektRequest.Inaktiv -> validerInntektskategori(Inntektskategori.INAKTIV)
                is InntektRequest.Arbeidsledig -> validerInntektskategori(Inntektskategori.ARBEIDSLEDIG)
            }

            yrkesaktivitetDao.oppdaterInntektrequest(yrkesaktivitet, request)

            fun hentPensjonsgivende(): List<HentPensjonsgivendeInntektResponse> =
                lastSigrunDokument(
                    periode = periode,
                    saksbehandlerToken = saksbehandler.token,
                    sigrunClient = sigrunClient,
                ).somPensjonsgivendeInntekt()

            val inntektData =
                when (request) {
                    is InntektRequest.Arbeidstaker -> {
                        yrkesaktivitetDao.oppdaterRefusjonsdata(yrkesaktivitet, request.data.refusjon)
                        when (request.data) {
                            is ArbeidstakerInntektRequest.Skjønnsfastsatt -> {
                                InntektData.ArbeidstakerSkjønnsfastsatt(
                                    omregnetÅrsinntekt = Inntekt.gjenopprett(request.data.årsinntekt).dto().årlig,
                                    sporing = "SKJØNNSFASTSATT_${request.data.årsak.name} TODO",
                                )
                            }

                            is ArbeidstakerInntektRequest.Ainntekt -> {
                                val omregnetÅrsinntekt =
                                    lastAInntektBeregningsgrunnlag(
                                        periode = periode,
                                        aInntektClient = aInntektClient,
                                        saksbehandler = saksbehandler,
                                    ).somAInntektBeregningsgrunnlag()
                                        .omregnetÅrsinntekt((yrkesaktivitet.kategorisering as YrkesaktivitetKategorisering.Arbeidstaker).orgnummer)
                                InntektData.ArbeidstakerAinntekt(
                                    omregnetÅrsinntekt = omregnetÅrsinntekt.first,
                                    sporing = "ARB_SPG_HOVEDREGEL",
                                    kildedata = omregnetÅrsinntekt.second,
                                )
                            }

                            is ArbeidstakerInntektRequest.Inntektsmelding -> {
                                val inntektsmelding =
                                    lastInntektsmeldingDokument(
                                        periode = periode,
                                        inntektsmeldingId = request.data.inntektsmeldingId,
                                        inntektsmeldingClient = inntektsmeldingClient,
                                        saksbehandler = saksbehandler,
                                    ).somInntektsmelding()
                                // TODO valider at fnr og arbeidsgiver stemmer med yrkesaktivitet og person
                                InntektData.ArbeidstakerInntektsmelding(
                                    omregnetÅrsinntekt =
                                        InntektbeløpDto
                                            .MånedligDouble(
                                                inntektsmelding.get("beregnetInntekt").asDouble(),
                                            ).tilInntekt()
                                            .dto()
                                            .årlig,
                                    inntektsmeldingId = request.data.inntektsmeldingId,
                                    inntektsmelding = inntektsmelding,
                                    sporing = "ARB_SPG_HOVEDREGEL",
                                )
                            }

                            is ArbeidstakerInntektRequest.ManueltBeregnet -> {
                                InntektData.ArbeidstakerManueltBeregnet(
                                    omregnetÅrsinntekt = request.data.årsinntekt,
                                )
                            }
                        }
                    }

                    is InntektRequest.SelvstendigNæringsdrivende ->
                        when (request.data) {
                            is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
                                val pensjonsgivendeInntekt = hentPensjonsgivende()

                                if (pensjonsgivendeInntekt.kanBeregnesEtter835()) {
                                    val beregnet =
                                        pensjonsgivendeInntekt.tilBeregnetPensjonsgivendeInntekt(periode.skjæringstidspunkt)
                                    InntektData.SelvstendigNæringsdrivendePensjonsgivende(
                                        omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                                        sporing = "SN_SPG_HOVEDREGEL",
                                        pensjonsgivendeInntekt = beregnet,
                                    )
                                } else {
                                    // Oppdater yrkesaktiviteten med en slags warning
                                    return@transactional
                                }
                            }

                            is PensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
                                InntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                )
                            }
                        }

                    is InntektRequest.Inaktiv ->
                        when (request.data) {
                            is PensjonsgivendeInntektRequest.PensjonsgivendeInntekt -> {
                                val pensjonsgivendeInntekt = hentPensjonsgivende()

                                if (pensjonsgivendeInntekt.kanBeregnesEtter835()) {
                                    val beregnet =
                                        pensjonsgivendeInntekt.tilBeregnetPensjonsgivendeInntekt(periode.skjæringstidspunkt)
                                    InntektData.InaktivPensjonsgivende(
                                        omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                                        sporing = "BEREGNINGSSPORINGVERDI",
                                        pensjonsgivendeInntekt = beregnet,
                                    )
                                } else {
                                    // Oppdater yrkesaktiviteten med en slags warning
                                    return@transactional
                                }
                            }

                            is PensjonsgivendeInntektRequest.Skjønnsfastsatt -> {
                                InntektData.InaktivSkjønnsfastsatt(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                )
                            }
                        }

                    is InntektRequest.Frilanser ->
                        when (request.data) {
                            is FrilanserInntektRequest.Ainntekt -> {
                                val ainntektBeregningsgrunnlag =
                                    lastAInntektBeregningsgrunnlag(
                                        periode = periode,
                                        aInntektClient = aInntektClient,
                                        saksbehandler = saksbehandler,
                                    ).somAInntektBeregningsgrunnlag()

                                // For frilanser henter vi all inntekt uten å filtrere på orgnummer
                                val inntektResponse = ainntektBeregningsgrunnlag.first.tilInntektApiUt()
                                val fom = ainntektBeregningsgrunnlag.second.fom
                                val tom = ainntektBeregningsgrunnlag.second.tom

                                val månederOgInntekt = monthsBetween(fom, tom).associateWith { Inntekt.INGEN }.toMutableMap()

                                inntektResponse.data.forEach { måned ->
                                    måned.inntektListe.forEach { inntekt ->
                                        månederOgInntekt[måned.maaned] =
                                            månederOgInntekt.getValue(måned.maaned) +
                                            Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(inntekt.beloep.toDouble()))
                                    }
                                }

                                val månedligSnitt = månederOgInntekt.values.summer().div(månederOgInntekt.size.toDouble())
                                val månederOgInntektDto = månederOgInntekt.mapValues { it.value.dto().månedligDouble }

                                InntektData.FrilanserAinntekt(
                                    omregnetÅrsinntekt = månedligSnitt.dto().årlig,
                                    sporing = "FRILANSER_SPG_HOVEDREGEL",
                                    kildedata = månederOgInntektDto,
                                )
                            }

                            is FrilanserInntektRequest.Skjønnsfastsatt -> {
                                InntektData.FrilanserSkjønnsfastsatt(
                                    omregnetÅrsinntekt = InntektbeløpDto.Årlig(400000.0),
                                    sporing = "A-inntekt TODO",
                                )
                            }
                        }

                    is InntektRequest.Arbeidsledig -> {
                        when (request.data) {
                            is ArbeidsledigInntektRequest.Dagpenger -> {
                                InntektData.Arbeidsledig(
                                    omregnetÅrsinntekt = Inntekt.gjenopprett(request.data.dagbeløp).dto().årlig,
                                )
                            }

                            is ArbeidsledigInntektRequest.Vartpenger ->
                                InntektData.Arbeidsledig(
                                    omregnetÅrsinntekt = Inntekt.gjenopprett(request.data.årsinntekt).dto().årlig,
                                )

                            is ArbeidsledigInntektRequest.Ventelønn ->
                                InntektData.Arbeidsledig(
                                    omregnetÅrsinntekt = Inntekt.gjenopprett(request.data.årsinntekt).dto().årlig,
                                )
                        }
                    }
                }

            yrkesaktivitetDao.oppdaterInntektData(yrkesaktivitet, inntektData)
            beregnSykepengegrunnlagOgUtbetaling(ref.saksbehandlingsperiodeReferanse, saksbehandler.bruker)?.let { rec ->
                requireNotNull(rec.sykepengegrunnlag)
                if (rec.sammenlikningsgrunnlag == null) {
                    val dokument =
                        lastAInntektSammenlikningsgrunnlag(periode, aInntektClient, saksbehandler)
                    val sammenlikningsgrunnlag = dokument.somAInntektSammenlikningsgrunnlag().sammenlikningsgrunnlag()

                    val avvikProsent =
                        if (sammenlikningsgrunnlag.beløp == 0.0) {
                            100.0
                        } else {
                            (
                                abs(rec.sykepengegrunnlag.totaltInntektsgrunnlag.beløp - sammenlikningsgrunnlag.beløp) /
                                    sammenlikningsgrunnlag.beløp
                            ) * 100.0
                        }

                    sykepengegrunnlagDao.oppdaterSammenlikningsgrunnlag(
                        sykepengegrunnlagId = rec.id,
                        sammenlikningsgrunnlag =
                            Sammenlikningsgrunnlag(
                                totaltSammenlikningsgrunnlag = sammenlikningsgrunnlag,
                                avvikProsent = avvikProsent,
                                avvikMotInntektsgrunnlag = rec.sykepengegrunnlag.totaltInntektsgrunnlag,
                                basertPåDokumentId = dokument.id,
                            ),
                    )
                }
            }
        }
    }

    fun hentPensjonsgivendeInntektForYrkesaktivitet(
        ref: YrkesaktivitetReferanse,
        saksbehandler: BrukerOgToken,
    ): PensjonsgivendeInntektResponse {
        return db.transactional {
            val periode =
                saksbehandlingsperiodeDao.hentPeriode(
                    ref = ref.saksbehandlingsperiodeReferanse,
                    krav = saksbehandler.bruker.erSaksbehandlerPåSaken(),
                )
            if (periode.skjæringstidspunkt == null) {
                return@transactional PensjonsgivendeInntektResponse.Feil(
                    feilmelding = "Kan ikke hente pensjonsgivende inntekt før skjæringstidspunkt er satt",
                )
            }

            val yrkesaktivitet =
                yrkesaktivitetDao.hentYrkesaktivitet(ref.yrkesaktivitetUUID)
                    ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")

            require(yrkesaktivitet.saksbehandlingsperiodeId == periode.id) {
                "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
            }

            val kategori = yrkesaktivitet.kategorisering.inntektskategori
            if (kategori != Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE && kategori != Inntektskategori.INAKTIV) {
                return@transactional PensjonsgivendeInntektResponse.Feil(
                    feilmelding = "Kan kun hente pensjonsgivende inntekt for selvstendig næringsdrivende eller inaktiv",
                )
            }

            try {
                val pensjonsgivendeInntekt =
                    lastSigrunDokument(
                        periode = periode,
                        saksbehandlerToken = saksbehandler.token,
                        sigrunClient = sigrunClient,
                    ).somPensjonsgivendeInntekt()

                if (!pensjonsgivendeInntekt.kanBeregnesEtter835()) {
                    return@transactional PensjonsgivendeInntektResponse.Feil(
                        feilmelding = "Mangler pensjonsgivende inntekt for de siste tre årene",
                    )
                }

                val beregnet = pensjonsgivendeInntekt.tilBeregnetPensjonsgivendeInntekt(periode.skjæringstidspunkt)

                when (kategori) {
                    Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE ->
                        PensjonsgivendeInntektResponse.Suksess(
                            data =
                                InntektData.SelvstendigNæringsdrivendePensjonsgivende(
                                    omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                                    sporing = "SN_SPG_HOVEDREGEL",
                                    pensjonsgivendeInntekt = beregnet,
                                ),
                        )

                    Inntektskategori.INAKTIV ->
                        PensjonsgivendeInntektResponse.Suksess(
                            data =
                                InntektData.InaktivPensjonsgivende(
                                    omregnetÅrsinntekt = beregnet.omregnetÅrsinntekt,
                                    sporing = "INAKTIV_SPG_HOVEDREGEL",
                                    pensjonsgivendeInntekt = beregnet,
                                ),
                        )

                    else -> throw IllegalStateException("Ugyldig kategori")
                }
            } catch (e: Exception) {
                PensjonsgivendeInntektResponse.Feil(
                    feilmelding = "Kunne ikke hente pensjonsgivende inntekt fra Sigrun: ${e.message}",
                )
            }
        }
    }

    fun hentAInntektForYrkesaktivitet(
        ref: YrkesaktivitetReferanse,
        saksbehandler: BrukerOgToken,
    ): AInntektResponse {
        return db.transactional {
            val periode =
                saksbehandlingsperiodeDao.hentPeriode(
                    ref = ref.saksbehandlingsperiodeReferanse,
                    krav = saksbehandler.bruker.erSaksbehandlerPåSaken(),
                )

            val yrkesaktivitet =
                yrkesaktivitetDao.hentYrkesaktivitet(ref.yrkesaktivitetUUID)
                    ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")

            require(yrkesaktivitet.saksbehandlingsperiodeId == periode.id) {
                "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
            }

            val kategori = yrkesaktivitet.kategorisering.inntektskategori
            if (kategori != Inntektskategori.ARBEIDSTAKER && kategori != Inntektskategori.FRILANSER) {
                return@transactional AInntektResponse.Feil(
                    feilmelding = "Kan kun hente a-inntekt for arbeidstaker eller frilanser",
                )
            }

            try {
                val ainntektBeregningsgrunnlag =
                    lastAInntektBeregningsgrunnlag(
                        periode = periode,
                        aInntektClient = aInntektClient,
                        saksbehandler = saksbehandler,
                    ).somAInntektBeregningsgrunnlag()

                when (kategori) {
                    Inntektskategori.ARBEIDSTAKER -> {
                        val orgnummer = (yrkesaktivitet.kategorisering as YrkesaktivitetKategorisering.Arbeidstaker).orgnummer
                        val omregnetÅrsinntekt = ainntektBeregningsgrunnlag.omregnetÅrsinntekt(orgnummer)

                        AInntektResponse.Suksess(
                            data =
                                InntektData.ArbeidstakerAinntekt(
                                    omregnetÅrsinntekt = omregnetÅrsinntekt.first,
                                    sporing = "ARB_SPG_HOVEDREGEL",
                                    kildedata = omregnetÅrsinntekt.second,
                                ),
                        )
                    }

                    Inntektskategori.FRILANSER -> {
                        // For frilanser henter vi all inntekt uten å filtrere på orgnummer
                        val inntektResponse = ainntektBeregningsgrunnlag.first.tilInntektApiUt()
                        val fom = ainntektBeregningsgrunnlag.second.fom
                        val tom = ainntektBeregningsgrunnlag.second.tom

                        val månederOgInntekt = monthsBetween(fom, tom).associateWith { Inntekt.INGEN }.toMutableMap()

                        inntektResponse.data.forEach { måned ->
                            måned.inntektListe.forEach { inntekt ->
                                månederOgInntekt[måned.maaned] =
                                    månederOgInntekt.getValue(måned.maaned) +
                                    Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(inntekt.beloep.toDouble()))
                            }
                        }

                        val månedligSnitt = månederOgInntekt.values.summer().div(månederOgInntekt.size.toDouble())
                        val månederOgInntektDto = månederOgInntekt.mapValues { it.value.dto().månedligDouble }

                        AInntektResponse.Suksess(
                            data =
                                InntektData.FrilanserAinntekt(
                                    omregnetÅrsinntekt = månedligSnitt.dto().årlig,
                                    sporing = "FRILANSER_SPG_HOVEDREGEL",
                                    kildedata = månederOgInntektDto,
                                ),
                        )
                    }

                    else -> throw IllegalStateException("Ugyldig kategori")
                }
            } catch (e: Exception) {
                AInntektResponse.Feil(
                    feilmelding = "Kunne ikke hente a-inntekt: ${e.message}",
                )
            }
        }
    }
}

sealed interface PensjonsgivendeInntektResponse {
    data class Suksess(
        val data: InntektData,
    ) : PensjonsgivendeInntektResponse

    data class Feil(
        val feilmelding: String,
    ) : PensjonsgivendeInntektResponse
}

sealed interface AInntektResponse {
    data class Suksess(
        val data: InntektData,
    ) : AInntektResponse

    data class Feil(
        val feilmelding: String,
    ) : AInntektResponse
}

private fun Pair<Inntektoppslag, AinntektPeriodeNøkkel>.omregnetÅrsinntekt(orgnummer: String): Pair<InntektbeløpDto.Årlig, Map<YearMonth, InntektbeløpDto.MånedligDouble>> {
    val inntektResponse = first.tilInntektApiUt()
    val fom = second.fom
    val tom = second.tom

    // map måned til 0 verdi
    val månederOgInntekt = monthsBetween(fom, tom).associateWith { Inntekt.INGEN }.toMutableMap()
    require(månederOgInntekt.size == 3)

    if (inntektResponse.data.any { !månederOgInntekt.contains(it.maaned) }) {
        throw IllegalStateException("Inntektsdata inneholder måneder utenfor forventet intervall: $fom - $tom")
    }

    inntektResponse.data
        .filter { it.underenhet == orgnummer }
        .forEach {
            it.inntektListe.forEach { inntekt ->
                månederOgInntekt[it.maaned] =
                    månederOgInntekt.getValue(it.maaned) + Inntekt.gjenopprett(InntektbeløpDto.MånedligDouble(inntekt.beloep.toDouble()))
            }
        }

    val månedligSnitt = månederOgInntekt.values.summer().div(månederOgInntekt.size.toDouble())
    val månederOgInntektDto = månederOgInntekt.mapValues { it.value.dto().månedligDouble }

    return Pair(månedligSnitt.dto().årlig, månederOgInntektDto)
}

private fun Pair<Inntektoppslag, AinntektPeriodeNøkkel>.sammenlikningsgrunnlag(): InntektbeløpDto.Årlig {
    val inntektResponse = first.tilInntektApiUt()
    val fom = second.fom
    val tom = second.tom

    // map måned til 0 verdi
    val månederOgInntekt = monthsBetween(fom, tom).associateWith { Inntekt.INGEN }.toMutableMap()
    require(månederOgInntekt.size == 12)

    if (inntektResponse.data.any { !månederOgInntekt.contains(it.maaned) }) {
        throw IllegalStateException("Inntektsdata inneholder måneder utenfor forventet intervall: $fom - $tom")
    }

    val inntektSiste12Måneder = inntektResponse.data.flatMap { it.inntektListe.map { inntekt -> inntekt.beloep.toDouble() } }.sum()

    return InntektbeløpDto.Årlig(inntektSiste12Måneder)
}

fun monthsBetween(
    fom: YearMonth,
    tom: YearMonth,
): List<YearMonth> {
    require(!tom.isBefore(fom)) { "tom ($tom) kan ikke være før fom ($fom)" }

    return generateSequence(fom) { prev ->
        if (prev.isBefore(tom)) prev.plusMonths(1) else null
    }.toList()
}
