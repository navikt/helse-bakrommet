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
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.tilDagoversiktJson
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsBeregningHjelper
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.toJsonNode
import java.time.OffsetDateTime
import java.util.*

data class InntektsforholdReferanse(
    val saksbehandlingsperiodeReferanse: SaksbehandlingsperiodeReferanse,
    val inntektsforholdUUID: UUID,
)

interface InntektsforholdServiceDaoer {
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val beregningDao: UtbetalingsberegningDao
}

typealias InntektsforholdKategorisering = JsonNode
typealias DagerSomSkalOppdateres = JsonNode

class InntektsforholdService(
    daoer: InntektsforholdServiceDaoer,
    sessionFactory: TransactionalSessionFactory<InntektsforholdServiceDaoer>,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    private fun InntektsforholdKategorisering.tilDatabaseType(
        behandlingsperiodeId: UUID,
        dagoversikt: List<Dag>?,
    ) = Yrkesaktivitet(
        id = UUID.randomUUID(),
        kategorisering = this,
        kategoriseringGenerert = null,
        dagoversikt = dagoversikt?.toJsonNode(),
        dagoversiktGenerert = null,
        saksbehandlingsperiodeId = behandlingsperiodeId,
        opprettet = OffsetDateTime.now(),
        generertFraDokumenter = emptyList(),
    )

    private fun InntektsforholdKategorisering.skalHaDagoversikt(): Boolean {
        val erSykmeldt = this.get("ER_SYKMELDT")?.asText()
        return erSykmeldt == "ER_SYKMELDT_JA" || erSykmeldt == null
    }

    fun hentInntektsforholdFor(ref: SaksbehandlingsperiodeReferanse): List<Yrkesaktivitet> =
        db.nonTransactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = null)
            yrkesaktivitetDao.hentYrkesaktivitetFor(periode)
        }

    fun opprettInntektsforhold(
        ref: SaksbehandlingsperiodeReferanse,
        kategorisering: InntektsforholdKategorisering,
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

            // Slett sykepengegrunnlag når inntektsforhold endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.periodeUUID)

            inntektsforhold
        }

    fun oppdaterKategorisering(
        ref: InntektsforholdReferanse,
        kategorisering: InntektsforholdKategorisering,
        saksbehandler: Bruker,
    ) {
        db.nonTransactional {
            val inntektsforhold =
                hentYrkesaktivitet(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            yrkesaktivitetDao.oppdaterKategorisering(inntektsforhold, kategorisering)

            // Slett sykepengegrunnlag når inntektsforhold endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.saksbehandlingsperiodeReferanse.periodeUUID)
        }
    }

    fun slettInntektsforhold(
        ref: InntektsforholdReferanse,
        saksbehandler: Bruker,
    ) {
        db.nonTransactional {
            val inntektsforhold =
                hentYrkesaktivitet(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            yrkesaktivitetDao.slettInntektsforhold(inntektsforhold.id)

            // Slett sykepengegrunnlag når inntektsforhold endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.saksbehandlingsperiodeReferanse.periodeUUID)
        }
    }

    fun oppdaterDagoversiktDager(
        ref: InntektsforholdReferanse,
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
            val eksisterendeDagoversikt = inntektsforhold.dagoversikt.tilDagoversiktJson()

            // Opprett map for enkel oppslag basert på dato
            val eksisterendeDagerMap =
                eksisterendeDagoversikt.associateBy {
                    it["dato"].asText()
                }.toMutableMap()

            // Oppdater kun dagene som finnes i input, ignorer helgedager
            dagerSomSkalOppdateresArray.forEach { oppdatertDagJson ->
                val dato = oppdatertDagJson["dato"].asText()
                val eksisterendeDag = eksisterendeDagerMap[dato]

                if (eksisterendeDag != null && eksisterendeDag["dagtype"].asText() != "Helg") {
                    // Oppdater dagen og sett kilde til Saksbehandler
                    val oppdatertDag =
                        objectMapper.createObjectNode().apply {
                            set<JsonNode>("dato", oppdatertDagJson["dato"])
                            set<JsonNode>("dagtype", oppdatertDagJson["dagtype"])
                            set<JsonNode>("grad", oppdatertDagJson["grad"])
                            set<JsonNode>("avvistBegrunnelse", oppdatertDagJson["avvistBegrunnelse"])
                            put("kilde", "Saksbehandler")
                        }
                    eksisterendeDagerMap[dato] = oppdatertDag
                }
            }

            // Konverter tilbake til JsonNode array og lagre
            val oppdatertDagoversikt =
                objectMapper.createArrayNode().apply {
                    eksisterendeDagerMap.values.forEach { add(it) }
                }

            val oppdatertInntektsforhold = yrkesaktivitetDao.oppdaterDagoversikt(inntektsforhold, oppdatertDagoversikt)

            val beregningshjelperISammeTransaksjon =
                UtbetalingsBeregningHjelper(
                    beregningDao,
                    saksbehandlingsperiodeDao,
                    sykepengegrunnlagDao,
                    yrkesaktivitetDao,
                )
            beregningshjelperISammeTransaksjon.settBeregning(ref.saksbehandlingsperiodeReferanse, saksbehandler)

            oppdatertInntektsforhold
        }
}

private fun InntektsforholdServiceDaoer.hentYrkesaktivitet(
    ref: InntektsforholdReferanse,
    krav: BrukerHarRollePåSakenKrav?,
) = saksbehandlingsperiodeDao.hentPeriode(
    ref = ref.saksbehandlingsperiodeReferanse,
    krav = krav,
).let { periode ->
    val inntektsforhold =
        yrkesaktivitetDao.hentYrkesaktivitet(ref.inntektsforholdUUID)
            ?: throw IkkeFunnetException("Inntektsforhold ikke funnet")
    require(inntektsforhold.saksbehandlingsperiodeId == periode.id) {
        "Inntektsforhold (id=${ref.inntektsforholdUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
    }
    inntektsforhold
}
