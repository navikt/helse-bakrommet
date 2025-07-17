package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
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
    private val daoer: InntektsforholdServiceDaoer,
    private val sessionFactory: TransactionalSessionFactory<InntektsforholdServiceDaoer>,
) {
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

    fun hentInntektsforholdFor(ref: SaksbehandlingsperiodeReferanse): List<Inntektsforhold> {
        val periode = daoer.saksbehandlingsperiodeDao.hentPeriode(ref)
        return daoer.inntektsforholdDao.hentInntektsforholdFor(periode)
    }

    fun opprettInntektsforhold(
        ref: SaksbehandlingsperiodeReferanse,
        kategorisering: InntektsforholdKategorisering,
    ): Inntektsforhold {
        val periode = daoer.saksbehandlingsperiodeDao.hentPeriode(ref)
        val dagoversikt =
            if (kategorisering.skalHaDagoversikt()) {
                initialiserDager(periode.fom, periode.tom)
            } else {
                null
            }
        val fraDatabasen =
            daoer.inntektsforholdDao.opprettInntektsforhold(kategorisering.tilDatabaseType(periode.id, dagoversikt))
        return fraDatabasen
    }

    fun oppdaterKategorisering(
        ref: InntektsforholdReferanse,
        kategorisering: InntektsforholdKategorisering,
    ) {
        val inntektsforhold = daoer.hentInntektsforhold(ref)
        daoer.inntektsforholdDao.oppdaterKategorisering(inntektsforhold, kategorisering)
    }

    fun oppdaterDagoversiktDager(
        ref: InntektsforholdReferanse,
        dagerSomSkalOppdateres: DagerSomSkalOppdateres,
    ): Inntektsforhold =
        sessionFactory.transactionalSessionScope { session ->
            val inntektsforhold = session.hentInntektsforhold(ref)
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

            session.inntektsforholdDao.oppdaterDagoversikt(inntektsforhold, oppdatertDagoversikt)
        }
}

private fun InntektsforholdServiceDaoer.hentInntektsforhold(ref: InntektsforholdReferanse) =
    saksbehandlingsperiodeDao.hentPeriode(ref.saksbehandlingsperiodeReferanse).let { periode ->
        val inntektsforhold =
            inntektsforholdDao.hentInntektsforhold(ref.inntektsforholdUUID)
                ?: throw IkkeFunnetException("Inntektsforhold ikke funnet")
        require(inntektsforhold.saksbehandlingsperiodeId == periode.id) {
            "Inntektsforhold (id=${ref.inntektsforholdUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
        }
        inntektsforhold
    }
