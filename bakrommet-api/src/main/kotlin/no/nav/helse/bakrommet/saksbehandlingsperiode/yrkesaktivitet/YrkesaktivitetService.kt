package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.saksbehandlingsperiode.BrukerHarRollePåSakenKrav
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsBeregningHjelper
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import java.time.OffsetDateTime
import java.util.*

data class YrkesaktivitetReferanse(
    val saksbehandlingsperiodeReferanse: SaksbehandlingsperiodeReferanse,
    val inntektsforholdUUID: UUID,
)

interface YrkesaktivitetServiceDaoer {
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val beregningDao: UtbetalingsberegningDao
}

typealias YrkesaktivitetKategorisering = Map<String, String>
typealias DagerSomSkalOppdateres = JsonNode

class YrkesaktivitetService(
    daoer: YrkesaktivitetServiceDaoer,
    sessionFactory: TransactionalSessionFactory<YrkesaktivitetServiceDaoer>,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    private fun YrkesaktivitetKategorisering.tilDatabaseType(
        behandlingsperiodeId: UUID,
        dagoversikt: List<Dag>?,
    ) = Yrkesaktivitet(
        id = UUID.randomUUID(),
        kategorisering = this,
        kategoriseringGenerert = null,
        dagoversikt = dagoversikt,
        dagoversiktGenerert = null,
        saksbehandlingsperiodeId = behandlingsperiodeId,
        opprettet = OffsetDateTime.now(),
        generertFraDokumenter = emptyList(),
        perioder = null,
    )

    private fun YrkesaktivitetKategorisering.skalHaDagoversikt(): Boolean {
        val erSykmeldt = this["ER_SYKMELDT"]
        return erSykmeldt == "ER_SYKMELDT_JA" || erSykmeldt == null
    }

    fun hentYrkesaktivitetFor(ref: SaksbehandlingsperiodeReferanse): List<Yrkesaktivitet> =
        db.nonTransactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = null)
            yrkesaktivitetDao.hentYrkesaktivitetFor(periode)
        }

    fun opprettYrkesaktivitet(
        ref: SaksbehandlingsperiodeReferanse,
        kategorisering: YrkesaktivitetKategorisering,
        saksbehandler: Bruker,
    ): Yrkesaktivitet =
        db.nonTransactional {
            val periode =
                saksbehandlingsperiodeDao.hentPeriode(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            val dagoversikt =
                if (kategorisering.skalHaDagoversikt()) {
                    initialiserDager(periode.fom, periode.tom)
                } else {
                    null
                }
            val inntektsforhold = yrkesaktivitetDao.opprettYrkesaktivitet(kategorisering.tilDatabaseType(periode.id, dagoversikt))

            // Slett sykepengegrunnlag og utbetalingsberegning når inntektsforhold endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.periodeUUID)
            beregningDao.slettBeregning(ref.periodeUUID)

            inntektsforhold
        }

    fun oppdaterKategorisering(
        ref: YrkesaktivitetReferanse,
        kategorisering: YrkesaktivitetKategorisering,
        saksbehandler: Bruker,
    ) {
        db.nonTransactional {
            val inntektsforhold =
                hentYrkesaktivitet(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            yrkesaktivitetDao.oppdaterKategorisering(inntektsforhold, kategorisering)

            // Slett sykepengegrunnlag og utbetalingsberegning når inntektsforhold endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.saksbehandlingsperiodeReferanse.periodeUUID)
            beregningDao.slettBeregning(ref.saksbehandlingsperiodeReferanse.periodeUUID)
        }
    }

    fun slettYrkesaktivitet(
        ref: YrkesaktivitetReferanse,
        saksbehandler: Bruker,
    ) {
        db.nonTransactional {
            val inntektsforhold =
                hentYrkesaktivitet(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            yrkesaktivitetDao.slettYrkesaktivitet(inntektsforhold.id)

            // Slett sykepengegrunnlag og utbetalingsberegning når inntektsforhold endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.saksbehandlingsperiodeReferanse.periodeUUID)
            beregningDao.slettBeregning(ref.saksbehandlingsperiodeReferanse.periodeUUID)
        }
    }

    fun oppdaterDagoversiktDager(
        ref: YrkesaktivitetReferanse,
        dagerSomSkalOppdateres: DagerSomSkalOppdateres,
        saksbehandler: Bruker,
    ): Yrkesaktivitet =
        db.transactional {
            val inntektsforhold =
                hentYrkesaktivitet(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            val dagerSomSkalOppdateresJson = dagerSomSkalOppdateres

            // Håndter både gammelt format (array av dager) og nytt format (objekt med dager og notat)
            val dagerSomSkalOppdateresArray =
                when {
                    dagerSomSkalOppdateresJson.isArray -> dagerSomSkalOppdateresJson
                    dagerSomSkalOppdateresJson.isObject && dagerSomSkalOppdateresJson.has("dager") && dagerSomSkalOppdateresJson.get("dager").isArray -> {
                        dagerSomSkalOppdateresJson.get("dager")
                    }
                    else -> throw InputValideringException("Body must be an array of days or an object with dager field")
                }

            // Hent eksisterende dagoversikt
            val eksisterendeDagoversikt = inntektsforhold.dagoversikt ?: emptyList()

            // Opprett map for enkel oppslag basert på dato
            val eksisterendeDagerMap =
                eksisterendeDagoversikt.associateBy { dag ->
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

            val oppdatertYrkesaktivitet = yrkesaktivitetDao.oppdaterDagoversikt(inntektsforhold, oppdatertDagoversikt)

            val beregningshjelperISammeTransaksjon =
                UtbetalingsBeregningHjelper(
                    beregningDao,
                    saksbehandlingsperiodeDao,
                    sykepengegrunnlagDao,
                    yrkesaktivitetDao,
                )
            beregningshjelperISammeTransaksjon.settBeregning(ref.saksbehandlingsperiodeReferanse, saksbehandler)

            oppdatertYrkesaktivitet
        }

    fun oppdaterPerioder(
        ref: YrkesaktivitetReferanse,
        perioder: Perioder?,
        saksbehandler: Bruker,
    ) {
        db.transactional {
            val inntektsforhold =
                hentYrkesaktivitet(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            yrkesaktivitetDao.oppdaterPerioder(inntektsforhold, perioder)

            val beregningshjelperISammeTransaksjon =
                UtbetalingsBeregningHjelper(
                    beregningDao,
                    saksbehandlingsperiodeDao,
                    sykepengegrunnlagDao,
                    yrkesaktivitetDao,
                )
            beregningshjelperISammeTransaksjon.settBeregning(ref.saksbehandlingsperiodeReferanse, saksbehandler)
        }
    }
}

private fun YrkesaktivitetServiceDaoer.hentYrkesaktivitet(
    ref: YrkesaktivitetReferanse,
    krav: BrukerHarRollePåSakenKrav?,
) = saksbehandlingsperiodeDao.hentPeriode(
    ref = ref.saksbehandlingsperiodeReferanse,
    krav = krav,
).let { periode ->
    val inntektsforhold =
        yrkesaktivitetDao.hentYrkesaktivitet(ref.inntektsforholdUUID)
            ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")
    require(inntektsforhold.saksbehandlingsperiodeId == periode.id) {
        "Yrkesaktivitet (id=${ref.inntektsforholdUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
    }
    inntektsforhold
}
