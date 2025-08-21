package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

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
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.toJsonNode
import java.time.OffsetDateTime
import java.util.*

data class YrkesaktivitetReferanse(
    val saksbehandlingsperiodeReferanse: SaksbehandlingsperiodeReferanse,
    val yrkesaktivitetUUID: UUID,
)

interface YrkesaktivitetServiceDaoer {
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val yrkesaktivitetDao: YrkesaktivitetDao
}

typealias YrkesaktivitetKategorisering = JsonNode
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
        dagoversikt = dagoversikt?.toJsonNode(),
        dagoversiktGenerert = null,
        saksbehandlingsperiodeId = behandlingsperiodeId,
        opprettet = OffsetDateTime.now(),
        generertFraDokumenter = emptyList(),
    )

    private fun YrkesaktivitetKategorisering.skalHaDagoversikt(): Boolean {
        val erSykmeldt = this.get("ER_SYKMELDT")?.asText()
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
            yrkesaktivitetDao.opprettYrkesaktivitet(kategorisering.tilDatabaseType(periode.id, dagoversikt))
        }

    fun oppdaterKategorisering(
        ref: YrkesaktivitetReferanse,
        kategorisering: YrkesaktivitetKategorisering,
        saksbehandler: Bruker,
    ) {
        db.nonTransactional {
            val yrkesaktivitet =
                hentYrkesaktivitet(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            yrkesaktivitetDao.oppdaterKategorisering(yrkesaktivitet, kategorisering)
        }
    }

    fun oppdaterDagoversiktDager(
        ref: YrkesaktivitetReferanse,
        dagerSomSkalOppdateres: DagerSomSkalOppdateres,
        saksbehandler: Bruker,
    ): Yrkesaktivitet =
        db.transactional {
            val yrkesaktivitet =
                hentYrkesaktivitet(
                    ref = ref,
                    krav = saksbehandler.erSaksbehandlerPåSaken(),
                )
            val dagerSomSkalOppdateresJson = dagerSomSkalOppdateres
            // Hent eksisterende dagoversikt
            val eksisterendeDagoversikt =
                yrkesaktivitet.dagoversikt?.let { dagoversiktJson ->
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

            yrkesaktivitetDao.oppdaterDagoversikt(yrkesaktivitet, oppdatertDagoversikt)
        }
}

private fun YrkesaktivitetServiceDaoer.hentYrkesaktivitet(
    ref: YrkesaktivitetReferanse,
    krav: BrukerHarRollePåSakenKrav?,
) = saksbehandlingsperiodeDao.hentPeriode(
    ref = ref.saksbehandlingsperiodeReferanse,
    krav = krav,
).let { periode ->
    val yrkesaktivitet =
        yrkesaktivitetDao.hentYrkesaktivitet(ref.yrkesaktivitetUUID)
            ?: throw IkkeFunnetException("Yrkesaktivitet ikke funnet")
    require(yrkesaktivitet.saksbehandlingsperiodeId == periode.id) {
        "Yrkesaktivitet (id=${ref.yrkesaktivitetUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
    }
    yrkesaktivitet
}
