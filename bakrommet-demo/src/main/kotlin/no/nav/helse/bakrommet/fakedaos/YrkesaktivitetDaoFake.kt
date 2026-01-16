package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetForenkletDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.tilYrkesaktivitet
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class YrkesaktivitetDaoFake : YrkesaktivitetDao {
    private val storage = ConcurrentHashMap<UUID, YrkesaktivitetDbRecord>()

    override fun opprettYrkesaktivitet(
        id: UUID,
        kategorisering: YrkesaktivitetKategorisering,
        dagoversikt: Dagoversikt?,
        behandlingId: UUID,
        opprettet: OffsetDateTime,
        generertFraDokumenter: List<UUID>,
        perioder: Perioder?,
        inntektData: InntektData?,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord {
        val record =
            YrkesaktivitetDbRecord(
                id = id,
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = dagoversikt,
                dagoversiktGenerert = null,
                behandlingId = behandlingId,
                opprettet = opprettet,
                generertFraDokumenter = generertFraDokumenter,
                perioder = perioder,
                inntektRequest = null,
                inntektData = inntektData,
                refusjon = refusjonsdata,
            )
        storage[id] = record
        return record
    }

    override fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord? = storage[id]

    override fun hentYrkesaktivitet(id: UUID): LegacyYrkesaktivitet? = storage[id]?.tilYrkesaktivitet()

    override fun hentYrkesaktiviteter(periode: BehandlingDbRecord): List<LegacyYrkesaktivitet> = storage.values.filter { it.behandlingId == periode.id }.map { it.tilYrkesaktivitet() }

    override fun hentYrkesaktiviteterDbRecord(periode: BehandlingDbRecord): List<YrkesaktivitetDbRecord> = hentYrkesaktiviteterDbRecord(periode.id)

    override fun hentYrkesaktiviteterDbRecord(behandlingId: UUID): List<YrkesaktivitetDbRecord> = storage.values.filter { it.behandlingId == behandlingId }

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
        oppdatertDagoversikt: Dagoversikt,
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
        legacyYrkesaktivitet: LegacyYrkesaktivitet,
        request: InntektRequest,
    ): YrkesaktivitetDbRecord {
        val eksisterende =
            storage[legacyYrkesaktivitet.id]
                ?: throw IllegalArgumentException("Fant ikke yrkesaktivitet med id ${legacyYrkesaktivitet.id}")
        val oppdatert = eksisterende.copy(inntektRequest = request)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterInntektData(
        legacyYrkesaktivitet: LegacyYrkesaktivitet,
        inntektData: InntektData,
    ): YrkesaktivitetDbRecord {
        val eksisterende =
            storage[legacyYrkesaktivitet.id]
                ?: throw IllegalArgumentException("Fant ikke yrkesaktivitet med id ${legacyYrkesaktivitet.id}")
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

    override fun finnYrkesaktiviteterForBehandlinger(map: List<UUID>): List<YrkesaktivitetForenkletDbRecord> =
        storage.values.filter { map.contains(it.behandlingId) }.map {
            YrkesaktivitetForenkletDbRecord(
                id = it.id,
                kategorisering = it.kategorisering,
                behandlingId = it.behandlingId,
            )
        }
}
