package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.*
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.behandling.beregning.beregnUtbetaling
import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.maybeOrgnummer
import no.nav.helse.bakrommet.ereg.EregClient
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.person.PersonPseudoIdDao
import no.nav.helse.bakrommet.util.logg
import java.time.OffsetDateTime
import java.util.*

data class YrkesaktivitetReferanse(
    val behandlingReferanse: BehandlingReferanse,
    val yrkesaktivitetUUID: UUID,
)

interface YrkesaktivitetServiceDaoer : Beregningsdaoer {
    override val behandlingDao: BehandlingDao
    override val yrkesaktivitetDao: YrkesaktivitetDao
    override val beregningDao: UtbetalingsberegningDao
    override val personPseudoIdDao: PersonPseudoIdDao
    override val sykepengegrunnlagDao: SykepengegrunnlagDao
    val behandlingEndringerDao: BehandlingEndringerDao
}

data class YrkesaktivitetMedOrgnavn(
    val yrkesaktivitet: YrkesaktivitetDbRecord,
    val orgnavn: String?,
)

class YrkesaktivitetService(
    private val db: DbDaoer<YrkesaktivitetServiceDaoer>,
    private val eregClient: EregClient,
) {
    private fun YrkesaktivitetKategorisering.skalHaDagoversikt(): Boolean = this.sykmeldt

    private suspend fun hentYrkesaktivitet(
        ref: YrkesaktivitetReferanse,
        krav: BrukerHarRollePåSakenKrav?,
        eksisterendeTranaksjon: YrkesaktivitetServiceDaoer? = null,
    ): YrkesaktivitetDbRecord =
        db.transactional(eksisterendeTranaksjon) {
            behandlingDao
                .hentPeriode(
                    ref = ref.behandlingReferanse,
                    krav = krav,
                ).let { periode ->
                    val yrkesaktivitet =
                        yrkesaktivitetDao.hentYrkesaktivitetDbRecord(ref.yrkesaktivitetUUID)
                            ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")
                    require(yrkesaktivitet.saksbehandlingsperiodeId == periode.id) {
                        "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
                    }
                    yrkesaktivitet
                }
        }

    suspend fun hentYrkesaktivitetFor(ref: BehandlingReferanse): List<YrkesaktivitetMedOrgnavn> =
        db.nonTransactional {
            val periode = behandlingDao.hentPeriode(ref, krav = null, måVæreUnderBehandling = false)
            val yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(periode)

            val alleOrgnummer = yrkesaktiviteter.mapNotNull { it.kategorisering.maybeOrgnummer() }.toSet()

            val organisasjonsnavnMap =
                coroutineScope {
                    alleOrgnummer
                        .associateWith { orgnummer ->
                            async {
                                withTimeoutOrNull(3_000) {
                                    try {
                                        eregClient.hentOrganisasjonsnavn(orgnummer)
                                    } catch (e: Exception) {
                                        logg.warn("Kall mot Ereg feilet for orgnummer $orgnummer", e)
                                        null
                                    }
                                }
                            }
                        }.mapValues { (_, deferred) -> deferred.await() }
                }

            yrkesaktiviteter.map { yrkesaktivitet ->
                YrkesaktivitetMedOrgnavn(
                    yrkesaktivitet = yrkesaktivitet,
                    orgnavn =
                        yrkesaktivitet.kategorisering
                            .maybeOrgnummer()
                            ?.let { organisasjonsnavnMap[it]?.navn },
                )
            }
        }

    suspend fun opprettYrkesaktivitet(
        ref: BehandlingReferanse,
        kategorisering: YrkesaktivitetKategorisering,
        saksbehandler: Bruker,
        eksisterendeTranaksjon: YrkesaktivitetServiceDaoer? = null,
    ): YrkesaktivitetMedOrgnavn =
        db.transactional(eksisterendeTranaksjon) {
            val orgnavn =
                kategorisering
                    .maybeOrgnummer()
                    ?.let { eregClient.hentOrganisasjonsnavn(it).navn }

            val periode =
                behandlingDao.hentPeriode(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            val dagoversikt =
                if (kategorisering.skalHaDagoversikt()) {
                    Dagoversikt(initialiserDager(periode.fom, periode.tom), emptyList())
                } else {
                    null
                }
            val yrkesaktivitet =
                yrkesaktivitetDao.opprettYrkesaktivitet(
                    id = UUID.randomUUID(),
                    kategorisering = kategorisering,
                    dagoversikt = dagoversikt,
                    saksbehandlingsperiodeId = periode.id,
                    opprettet = OffsetDateTime.now(),
                    generertFraDokumenter = emptyList(),
                    perioder = null,
                    inntektData = null,
                    refusjonsdata = null,
                )

            YrkesaktivitetMedOrgnavn(yrkesaktivitet, orgnavn)
        }

    suspend fun oppdaterKategorisering(
        ref: YrkesaktivitetReferanse,
        kategorisering: YrkesaktivitetKategorisering,
        saksbehandler: Bruker,
        eksisterendeTranaksjon: YrkesaktivitetServiceDaoer? = null,
    ) {
        // Validerer at organisasjon finnes hvis orgnummer er satt
        kategorisering
            .maybeOrgnummer()
            ?.let { eregClient.hentOrganisasjonsnavn(it).navn }

        val yrkesaktivtet = hentYrkesaktivitet(ref, saksbehandler.erSaksbehandlerPåSaken())
        val gammelKategorisering = yrkesaktivtet.kategorisering
        val hovedkategoriseringEndret = hovedkategoriseringEndret(gammelKategorisering, kategorisering)

        db.transactional(eksisterendeTranaksjon) {
            yrkesaktivitetDao.oppdaterKategoriseringOgSlettInntektData(yrkesaktivtet, kategorisering)

            // Hvis kategoriseringen endrer seg så må vi slette inntektdata og inntektrequest og beregning
            // Slett sykepengegrunnlag og utbetalingsberegning når yrkesaktivitet endres
            // Vi må alltid beregne på nytt når kategorisering endres
            beregningDao.slettBeregning(ref.behandlingReferanse.behandlingId, failSilently = true)

            // hvis sykepengegrunnlaget eies av denne perioden, slett det
            val periode =
                behandlingDao.hentPeriode(ref.behandlingReferanse, krav = saksbehandler.erSaksbehandlerPåSaken())
            periode.sykepengegrunnlagId?.let { sykepengegrunnlagId ->
                val spgRecord = sykepengegrunnlagDao.hentSykepengegrunnlag(sykepengegrunnlagId)
                if (spgRecord.opprettetForBehandling == periode.id) {
                    behandlingDao.oppdaterSykepengegrunnlagId(periode.id, null)
                    sykepengegrunnlagDao.slettSykepengegrunnlag(sykepengegrunnlagId)
                }
            }

            // Legg til endring i historikk hvis hovedkategorisering endrer seg
            if (hovedkategoriseringEndret) {
                behandlingEndringerDao.leggTilEndring(
                    SaksbehandlingsperiodeEndring(
                        saksbehandlingsperiodeId = periode.id,
                        status = periode.status,
                        beslutterNavIdent = periode.beslutterNavIdent,
                        endretTidspunkt = OffsetDateTime.now(),
                        endretAvNavIdent = saksbehandler.navIdent,
                        endringType = SaksbehandlingsperiodeEndringType.OPPDATERT_YRKESAKTIVITET_KATEGORISERING,
                        endringKommentar = "Endret fra ${hovedkategoriseringNavn(gammelKategorisering)} til ${
                            hovedkategoriseringNavn(
                                kategorisering,
                            )
                        }",
                    ),
                )
            }
        }
    }

    private fun hovedkategoriseringEndret(
        gammel: YrkesaktivitetKategorisering,
        ny: YrkesaktivitetKategorisering,
    ): Boolean = hovedkategoriseringNavn(gammel) != hovedkategoriseringNavn(ny)

    private fun hovedkategoriseringNavn(kategorisering: YrkesaktivitetKategorisering): String =
        when (kategorisering) {
            is YrkesaktivitetKategorisering.Arbeidstaker -> "Arbeidstaker"
            is YrkesaktivitetKategorisering.Frilanser -> "Frilanser"
            is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> "Selvstendig næringsdrivende"
            is YrkesaktivitetKategorisering.Inaktiv -> "Inaktiv"
            is YrkesaktivitetKategorisering.Arbeidsledig -> "Arbeidsledig"
        }

    suspend fun slettYrkesaktivitet(
        ref: YrkesaktivitetReferanse,
        saksbehandler: Bruker,
    ) {
        val yrkesaktivitet = hentYrkesaktivitet(ref, saksbehandler.erSaksbehandlerPåSaken())
        db.transactional {
            yrkesaktivitetDao.slettYrkesaktivitet(yrkesaktivitet.id)
            beregnSykepengegrunnlagOgUtbetaling(
                ref.behandlingReferanse,
                saksbehandler = saksbehandler,
            )
        }
    }

    suspend fun oppdaterDagoversiktDager(
        ref: YrkesaktivitetReferanse,
        dagerSomSkalOppdateres: List<Dag>,
        saksbehandler: Bruker,
    ): YrkesaktivitetDbRecord {
        val yrkesaktivitet = hentYrkesaktivitet(ref, saksbehandler.erSaksbehandlerPåSaken())
        dagerSomSkalOppdateres.validerAvslagsgrunn()
        return db.transactional {
            val oppdatertDagoversikt = dagerSomSkalOppdateres.applikerSaksbehandlerDagoppdateringer(yrkesaktivitet.dagoversikt)
            val oppdatertYrkesaktivitet = yrkesaktivitetDao.oppdaterDagoversikt(yrkesaktivitet, oppdatertDagoversikt)

            beregnUtbetaling(ref.behandlingReferanse, saksbehandler)
            oppdatertYrkesaktivitet
        }
    }

    suspend fun oppdaterPerioder(
        ref: YrkesaktivitetReferanse,
        perioder: Perioder?,
        saksbehandler: Bruker,
        eksisterendeTranaksjon: YrkesaktivitetServiceDaoer? = null,
    ) {
        db.transactional(eksisterendeTranaksjon) {
            val yrkesaktivitet = hentYrkesaktivitet(ref, saksbehandler.erSaksbehandlerPåSaken(), eksisterendeTranaksjon)
            yrkesaktivitetDao.oppdaterPerioder(yrkesaktivitet, perioder)
            beregnUtbetaling(ref.behandlingReferanse, saksbehandler)
        }
    }

    suspend fun oppdaterRefusjon(
        ref: YrkesaktivitetReferanse,
        refusjon: List<Refusjonsperiode>?,
        saksbehandler: Bruker,
    ) {
        val yrkesaktivitet = hentYrkesaktivitet(ref, saksbehandler.erSaksbehandlerPåSaken())
        db.transactional {
            yrkesaktivitetDao.oppdaterRefusjon(yrkesaktivitet.id, refusjon)
            beregnUtbetaling(ref.behandlingReferanse, saksbehandler)
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
