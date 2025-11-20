package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingEndringerDao
import no.nav.helse.bakrommet.behandling.BrukerHarRollePåSakenKrav
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndring
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringType
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.behandling.beregning.beregnUtbetaling
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.dagoversikt.Kilde
import no.nav.helse.bakrommet.behandling.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.person.PersonDao
import java.time.OffsetDateTime
import java.util.*

data class YrkesaktivitetReferanse(
    val saksbehandlingsperiodeReferanse: SaksbehandlingsperiodeReferanse,
    val yrkesaktivitetUUID: UUID,
)

interface YrkesaktivitetServiceDaoer : Beregningsdaoer {
    override val behandlingDao: BehandlingDao
    override val yrkesaktivitetDao: YrkesaktivitetDao
    override val beregningDao: UtbetalingsberegningDao
    override val personDao: PersonDao
    override val sykepengegrunnlagDao: SykepengegrunnlagDao
    val behandlingEndringerDao: BehandlingEndringerDao
}

typealias DagerSomSkalOppdateres = JsonNode

class YrkesaktivitetService(
    private val db: DbDaoer<YrkesaktivitetServiceDaoer>,
) {
    private fun YrkesaktivitetKategorisering.skalHaDagoversikt(): Boolean = this.sykmeldt

    private suspend fun hentYrkesaktivitet(
        ref: YrkesaktivitetReferanse,
        krav: BrukerHarRollePåSakenKrav?,
    ): YrkesaktivitetDbRecord =
        db.nonTransactional {
            behandlingDao
                .hentPeriode(
                    ref = ref.saksbehandlingsperiodeReferanse,
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

    suspend fun hentYrkesaktivitetFor(ref: SaksbehandlingsperiodeReferanse): List<YrkesaktivitetDbRecord> =
        db.nonTransactional {
            val periode = behandlingDao.hentPeriode(ref, krav = null, måVæreUnderBehandling = false)
            yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(periode)
        }

    suspend fun opprettYrkesaktivitet(
        ref: SaksbehandlingsperiodeReferanse,
        kategorisering: YrkesaktivitetKategorisering,
        saksbehandler: Bruker,
    ): YrkesaktivitetDbRecord =
        db.nonTransactional {
            val periode =
                behandlingDao.hentPeriode(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            val dagoversikt =
                if (kategorisering.skalHaDagoversikt()) {
                    initialiserDager(periode.fom, periode.tom)
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

            yrkesaktivitet
        }

    suspend fun oppdaterKategorisering(
        ref: YrkesaktivitetReferanse,
        kategorisering: YrkesaktivitetKategorisering,
        saksbehandler: Bruker,
    ) {
        val yrkesaktivtet = hentYrkesaktivitet(ref, saksbehandler.erSaksbehandlerPåSaken())
        val gammelKategorisering = yrkesaktivtet.kategorisering
        val hovedkategoriseringEndret = hovedkategoriseringEndret(gammelKategorisering, kategorisering)

        db.nonTransactional {
            yrkesaktivitetDao.oppdaterKategoriseringOgSlettInntektData(yrkesaktivtet, kategorisering)

            // Hvis kategoriseringen endrer seg så må vi slette inntektdata og inntektrequest og beregning
            // Slett sykepengegrunnlag og utbetalingsberegning når yrkesaktivitet endres
            // Vi må alltid beregne på nytt når kategorisering endres
            beregningDao.slettBeregning(ref.saksbehandlingsperiodeReferanse.periodeUUID)

            // hvis sykepengegrunnlaget eies av denne perioden, slett det
            val periode = behandlingDao.hentPeriode(ref.saksbehandlingsperiodeReferanse, krav = saksbehandler.erSaksbehandlerPåSaken())
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
                        endringKommentar = "Endret fra ${hovedkategoriseringNavn(gammelKategorisering)} til ${hovedkategoriseringNavn(kategorisering)}",
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
                ref.saksbehandlingsperiodeReferanse,
                saksbehandler = saksbehandler,
            )
        }
    }

    suspend fun oppdaterDagoversiktDager(
        ref: YrkesaktivitetReferanse,
        dagerSomSkalOppdateres: DagerSomSkalOppdateres,
        saksbehandler: Bruker,
    ): YrkesaktivitetDbRecord {
        val yrkesaktivitet = hentYrkesaktivitet(ref, saksbehandler.erSaksbehandlerPåSaken())
        return db.transactional {
            val dagerSomSkalOppdateresJson = dagerSomSkalOppdateres

            // Håndter både gammelt format (array av dager) og nytt format (objekt med dager og notat)
            val dagerSomSkalOppdateresArray =
                when {
                    dagerSomSkalOppdateresJson.isArray -> dagerSomSkalOppdateresJson
                    dagerSomSkalOppdateresJson.isObject &&
                        dagerSomSkalOppdateresJson.has("dager") &&
                        dagerSomSkalOppdateresJson
                            .get(
                                "dager",
                            ).isArray -> {
                        dagerSomSkalOppdateresJson.get("dager")
                    }

                    else -> throw InputValideringException("Body must be an array of days or an object with dager field")
                }

            // Hent eksisterende dagoversikt
            val eksisterendeDagoversikt = yrkesaktivitet.dagoversikt ?: emptyList()

            // Opprett map for enkel oppslag basert på dato
            val eksisterendeDagerMap =
                eksisterendeDagoversikt
                    .associateBy { dag ->
                        dag.dato.toString()
                    }.toMutableMap()

            // Oppdater kun dagene som finnes i input, ignorer helgedager
            dagerSomSkalOppdateresArray.forEach { oppdatertDagJson ->
                val dato = oppdatertDagJson["dato"].asText()
                val eksisterendeDag = eksisterendeDagerMap[dato]

                if (eksisterendeDag != null) {
                    // Oppdater dagen og sett kilde til Saksbehandler
                    val oppdatertDag =
                        eksisterendeDag.copy(
                            dagtype = Dagtype.valueOf(oppdatertDagJson["dagtype"].asText()),
                            grad =
                                if (oppdatertDagJson.has("grad") && !oppdatertDagJson["grad"].isNull) {
                                    oppdatertDagJson["grad"].asInt()
                                } else {
                                    null
                                },
                            avslåttBegrunnelse =
                                if (oppdatertDagJson.has("avslåttBegrunnelse") && !oppdatertDagJson["avslåttBegrunnelse"].isNull) {
                                    oppdatertDagJson["avslåttBegrunnelse"].map { it.asText() }
                                } else {
                                    null
                                },
                            kilde = Kilde.Saksbehandler,
                        )
                    eksisterendeDagerMap[dato] = oppdatertDag
                }
            }

            // Konverter tilbake til List<Dag> og lagre
            val oppdatertDagoversikt = eksisterendeDagerMap.values.toList()

            val oppdatertYrkesaktivitet = yrkesaktivitetDao.oppdaterDagoversikt(yrkesaktivitet, oppdatertDagoversikt)

            beregnUtbetaling(ref.saksbehandlingsperiodeReferanse, saksbehandler)
            oppdatertYrkesaktivitet
        }
    }

    suspend fun oppdaterPerioder(
        ref: YrkesaktivitetReferanse,
        perioder: Perioder?,
        saksbehandler: Bruker,
    ) {
        val yrkesaktivitet = hentYrkesaktivitet(ref, saksbehandler.erSaksbehandlerPåSaken())
        db.transactional {
            yrkesaktivitetDao.oppdaterPerioder(yrkesaktivitet, perioder)
            beregnUtbetaling(ref.saksbehandlingsperiodeReferanse, saksbehandler)
        }
    }

    suspend fun oppdaterRefusjon(
        ref: YrkesaktivitetReferanse,
        refusjon: List<Refusjonsperiode>?,
        saksbehandler: Bruker,
    ) {
        val yrkesaktivitet = hentYrkesaktivitet(ref, saksbehandler.erSaksbehandlerPåSaken())
        db.nonTransactional {
            yrkesaktivitetDao.oppdaterRefusjon(yrkesaktivitet.id, refusjon)
        }
    }
}
