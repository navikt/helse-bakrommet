package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.saksbehandlingsperiode.BrukerHarRollePåSakenKrav
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
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
    val inntektsforholdDao: InntektsforholdDao
}

typealias InntektsforholdKategorisering = JsonNode
typealias DagerSomSkalOppdateres = JsonNode

class InntektsforholdService(
    daoer: InntektsforholdServiceDaoer,
    sessionFactory: TransactionalSessionFactory<InntektsforholdServiceDaoer>,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    private fun InntektsforholdKategorisering.tilDatabaseType(
        behandlingsperiodeId: UUID,
        dagoversikt: List<Dag>?,
    ) = Inntektsforhold(
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

    fun hentInntektsforholdFor(ref: SaksbehandlingsperiodeReferanse): List<Inntektsforhold> =
        db.nonTransactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = null)
            inntektsforholdDao.hentInntektsforholdFor(periode)
        }

    fun opprettInntektsforhold(
        ref: SaksbehandlingsperiodeReferanse,
        kategorisering: InntektsforholdKategorisering,
        saksbehandler: Bruker,
    ): Inntektsforhold =
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
            val inntektsforhold = inntektsforholdDao.opprettInntektsforhold(kategorisering.tilDatabaseType(periode.id, dagoversikt))

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
                hentInntektsforhold(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            inntektsforholdDao.oppdaterKategorisering(inntektsforhold, kategorisering)

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
                hentInntektsforhold(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            inntektsforholdDao.slettInntektsforhold(inntektsforhold.id)

            // Slett sykepengegrunnlag når inntektsforhold endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.saksbehandlingsperiodeReferanse.periodeUUID)
        }
    }

    fun oppdaterDagoversiktDager(
        ref: InntektsforholdReferanse,
        dagerSomSkalOppdateres: DagerSomSkalOppdateres,
        saksbehandler: Bruker,
    ): Inntektsforhold =
        db.transactional {
            val inntektsforhold =
                hentInntektsforhold(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            val dagerSomSkalOppdateresJson = dagerSomSkalOppdateres
            // Hent eksisterende dagoversikt
            val eksisterendeDagoversikt =
                inntektsforhold.dagoversikt?.let { dagoversiktJson ->
                    if (dagoversiktJson.isArray) {
                        dagoversiktJson.toList()
                    } else {
                        emptyList()
                    }
                } ?: emptyList()

            // Opprett map for enkel oppslag basert på dato
            val eksisterendeDagerMap =
                eksisterendeDagoversikt.associateBy {
                    it["dato"].asText()
                }.toMutableMap()

            // Oppdater kun dagene som finnes i input, ignorer helgedager
            if (dagerSomSkalOppdateresJson.isArray) {
                dagerSomSkalOppdateresJson.forEach { oppdatertDagJson ->
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
            }

            // Konverter tilbake til JsonNode array og lagre
            val oppdatertDagoversikt =
                objectMapper.createArrayNode().apply {
                    eksisterendeDagerMap.values.forEach { add(it) }
                }

            val oppdatertInntektsforhold = inntektsforholdDao.oppdaterDagoversikt(inntektsforhold, oppdatertDagoversikt)

            // Slett sykepengegrunnlag når inntektsforhold endres
            sykepengegrunnlagDao.slettSykepengegrunnlag(ref.saksbehandlingsperiodeReferanse.periodeUUID)

            oppdatertInntektsforhold
        }
}

private fun InntektsforholdServiceDaoer.hentInntektsforhold(
    ref: InntektsforholdReferanse,
    krav: BrukerHarRollePåSakenKrav?,
) = saksbehandlingsperiodeDao.hentPeriode(
    ref = ref.saksbehandlingsperiodeReferanse,
    krav = krav,
).let { periode ->
    val inntektsforhold =
        inntektsforholdDao.hentInntektsforhold(ref.inntektsforholdUUID)
            ?: throw IkkeFunnetException("Inntektsforhold ikke funnet")
    require(inntektsforhold.saksbehandlingsperiodeId == periode.id) {
        "Inntektsforhold (id=${ref.inntektsforholdUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
    }
    inntektsforhold
}
