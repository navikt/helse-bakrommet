package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetForenkletDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.tilYrkesaktivitet
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class YrkesaktivitetDaoFake : YrkesaktivitetDao {
    private val storage = ConcurrentHashMap<UUID, YrkesaktivitetDbRecord>()

    override fun opprettYrkesaktivitet(
        id: UUID,
        kategorisering: YrkesaktivitetKategorisering,
        dagoversikt: List<Dag>?,
        saksbehandlingsperiodeId: UUID,
        opprettet: OffsetDateTime,
        generertFraDokumenter: List<UUID>,
        perioder: Perioder?,
        inntektData: InntektData?,
        refusjonsdata: List<Refusjonsperiode>?,
        inntekt: java.math.BigDecimal?,
    ): YrkesaktivitetDbRecord {
        val record =
            YrkesaktivitetDbRecord(
                id = id,
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = dagoversikt,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = saksbehandlingsperiodeId,
                opprettet = opprettet,
                generertFraDokumenter = generertFraDokumenter,
                perioder = perioder,
                inntektRequest = null,
                inntektData = inntektData,
                refusjon = refusjonsdata,
                inntekt = inntekt,
            )
        storage[id] = record
        return record
    }

    override fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord? = storage[id]

    override fun hentYrkesaktivitet(id: UUID): Yrkesaktivitet? = storage[id]?.tilYrkesaktivitet()

    override fun hentYrkesaktiviteter(periode: Behandling): List<Yrkesaktivitet> = storage.values.filter { it.saksbehandlingsperiodeId == periode.id }.map { it.tilYrkesaktivitet() }

    override fun hentYrkesaktiviteterDbRecord(periode: Behandling): List<YrkesaktivitetDbRecord> = storage.values.filter { it.saksbehandlingsperiodeId == periode.id }

    override fun oppdaterKategoriseringOgSlettInntektData(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        kategorisering: YrkesaktivitetKategorisering,
    ): YrkesaktivitetDbRecord {
        val eksisterende =
            storage[yrkesaktivitetDbRecord.id] ?: throw IllegalArgumentException("Kunne ikke finne yrkesaktivitet")
        val oppdatert = eksisterende.copy(kategorisering = kategorisering, inntektData = null, inntektRequest = null)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterDagoversikt(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        oppdatertDagoversikt: List<Dag>,
    ): YrkesaktivitetDbRecord {
        val eksisterende =
            storage[yrkesaktivitetDbRecord.id]
                ?: throw IllegalArgumentException("Fant ikke yrkesaktivitet med id ${yrkesaktivitetDbRecord.id}")
        val oppdatert = eksisterende.copy(dagoversikt = oppdatertDagoversikt)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterPerioder(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        perioder: Perioder?,
    ): YrkesaktivitetDbRecord {
        val eksisterende =
            storage[yrkesaktivitetDbRecord.id]
                ?: throw IllegalArgumentException("Fant ikke yrkesaktivitet med id ${yrkesaktivitetDbRecord.id}")
        val oppdatert = eksisterende.copy(perioder = perioder)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun slettYrkesaktivitet(id: UUID) {
        storage.remove(id)
    }

    override fun oppdaterInntektrequest(
        yrkesaktivitet: Yrkesaktivitet,
        request: InntektRequest,
    ): YrkesaktivitetDbRecord {
        val eksisterende =
            storage[yrkesaktivitet.id]
                ?: throw IllegalArgumentException("Fant ikke yrkesaktivitet med id ${yrkesaktivitet.id}")
        val oppdatert = eksisterende.copy(inntektRequest = request)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterInntektData(
        yrkesaktivitet: Yrkesaktivitet,
        inntektData: InntektData,
    ): YrkesaktivitetDbRecord {
        val eksisterende =
            storage[yrkesaktivitet.id]
                ?: throw IllegalArgumentException("Fant ikke yrkesaktivitet med id ${yrkesaktivitet.id}")
        val oppdatert = eksisterende.copy(inntektData = inntektData)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterRefusjon(
        yrkesaktivitetID: UUID,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord {
        val eksisterende =
            storage[yrkesaktivitetID]
                ?: throw IllegalArgumentException("Fant ikke yrkesaktivitet med id $yrkesaktivitetID")
        val oppdatert = eksisterende.copy(refusjon = refusjonsdata)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterInntekt(
        yrkesaktivitetID: UUID,
        inntekt: java.math.BigDecimal?,
    ): YrkesaktivitetDbRecord {
        val eksisterende =
            storage[yrkesaktivitetID]
                ?: throw IllegalArgumentException("Fant ikke yrkesaktivitet med id $yrkesaktivitetID")
        val oppdatert = eksisterende.copy(inntekt = inntekt)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun finnYrkesaktiviteterForBehandlinger(map: List<UUID>): List<YrkesaktivitetForenkletDbRecord> =
        storage.values.filter { map.contains(it.saksbehandlingsperiodeId) }.map {
            YrkesaktivitetForenkletDbRecord(
                id = it.id,
                kategorisering = it.kategorisering,
                behandlingId = it.saksbehandlingsperiodeId,
            )
        }
}
