package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.util.toJsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class InntektsforholdReferanse(
    val saksbehandlingsperiodeReferanse: SaksbehandlingsperiodeReferanse,
    val inntektsforholdUUID: UUID,
)

typealias InntektsforholdKategorisering = JsonNode
typealias DagerSomSkalOppdateres = JsonNode

class InntektsforholdService(
    private val inntektsforholdDao: InntektsforholdDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
) {
    private val daoer = Pair(saksbehandlingsperiodeDao, inntektsforholdDao)

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
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref)
        return inntektsforholdDao.hentInntektsforholdFor(periode)
    }

    fun opprettInntektsforhold(
        ref: SaksbehandlingsperiodeReferanse,
        kategorisering: InntektsforholdKategorisering,
    ): Inntektsforhold {
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref)
        val dagoversikt =
            if (kategorisering.skalHaDagoversikt()) {
                initialiserDager(periode.fom, periode.tom)
            } else {
                null
            }
        val fraDatabasen =
            inntektsforholdDao.opprettInntektsforhold(kategorisering.tilDatabaseType(periode.id, dagoversikt))
        return fraDatabasen
    }

    fun oppdaterDagoversiktDager(
        ref: InntektsforholdReferanse,
        dagerSomSkalOppdateres: DagerSomSkalOppdateres,
    ) {
        val inntektsforhold = daoer.hentInntektsforhold(ref)
        inntektsforholdDao.oppdaterDagoversiktDager(inntektsforhold, dagerSomSkalOppdateres)
    }

    fun oppdaterKategorisering(
        ref: InntektsforholdReferanse,
        kategorisering: InntektsforholdKategorisering,
    ) {
        val inntektsforhold = daoer.hentInntektsforhold(ref)
        inntektsforholdDao.oppdaterKategorisering(inntektsforhold, kategorisering)
    }
}

fun Pair<SaksbehandlingsperiodeDao, InntektsforholdDao>.hentInntektsforhold(ref: InntektsforholdReferanse) =
    this.let { (periodeDao, inntektsforholdDao) ->
        periodeDao.hentPeriode(ref.saksbehandlingsperiodeReferanse).let { periode ->
            val inntektsforhold =
                inntektsforholdDao.hentInntektsforhold(ref.inntektsforholdUUID)
                    ?: throw IkkeFunnetException("Inntektsforhold ikke funnet")
            require(inntektsforhold.saksbehandlingsperiodeId == periode.id) {
                "Inntektsforhold (id=${ref.inntektsforholdUUID}) tilhører ikke behandlingsperiode (id=${periode.id})"
            }
            inntektsforhold
        }
    }
