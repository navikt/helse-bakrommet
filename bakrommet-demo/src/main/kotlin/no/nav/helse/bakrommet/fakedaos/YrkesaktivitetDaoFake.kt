package no.nav.helse.bakrommet.fakedaos

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.hentSessionid
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.*
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class YrkesaktivitetDaoFake : YrkesaktivitetDao {
    // Map av sessionId -> yrkesaktivitetId -> YrkesaktivitetDbRecord
    private val sessionData = ConcurrentHashMap<String, ConcurrentHashMap<UUID, YrkesaktivitetDbRecord>>()

    private fun getSessionMap(): ConcurrentHashMap<UUID, YrkesaktivitetDbRecord> =
        runBlocking {
            val sessionId = hentSessionid()
            sessionData.getOrPut(sessionId) { ConcurrentHashMap() }
        }

    private val storage: ConcurrentHashMap<UUID, YrkesaktivitetDbRecord>
        get() = getSessionMap()

    override suspend fun opprettYrkesaktivitet(
        id: UUID,
        kategorisering: YrkesaktivitetKategorisering,
        dagoversikt: List<Dag>?,
        saksbehandlingsperiodeId: UUID,
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
                saksbehandlingsperiodeId = saksbehandlingsperiodeId,
                opprettet = opprettet,
                generertFraDokumenter = generertFraDokumenter,
                perioder = perioder,
                inntektRequest = null,
                inntektData = inntektData,
                refusjonsdata = refusjonsdata,
            )
        storage[id] = record
        return record
    }

    override suspend fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord? = storage[id]

    override suspend fun hentYrkesaktivitet(id: UUID): Yrkesaktivitet? = storage[id]?.tilYrkesaktivitet()

    override suspend fun hentYrkesaktiviteter(periode: Saksbehandlingsperiode): List<Yrkesaktivitet> = storage.values.filter { it.saksbehandlingsperiodeId == periode.id }.map { it.tilYrkesaktivitet() }

    override suspend fun hentYrkesaktiviteterDbRecord(periode: Saksbehandlingsperiode): List<YrkesaktivitetDbRecord> = storage.values.filter { it.saksbehandlingsperiodeId == periode.id }

    override suspend fun oppdaterKategorisering(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        kategorisering: YrkesaktivitetKategorisering,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitetDbRecord.id] ?: return yrkesaktivitetDbRecord
        val oppdatert = eksisterende.copy(kategorisering = kategorisering)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override suspend fun oppdaterDagoversikt(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        oppdatertDagoversikt: List<Dag>,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitetDbRecord.id] ?: return yrkesaktivitetDbRecord
        val oppdatert = eksisterende.copy(dagoversikt = oppdatertDagoversikt)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override suspend fun oppdaterPerioder(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        perioder: Perioder?,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitetDbRecord.id] ?: return yrkesaktivitetDbRecord
        val oppdatert = eksisterende.copy(perioder = perioder)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override suspend fun slettYrkesaktivitet(id: UUID) {
        storage.remove(id)
    }

    override suspend fun oppdaterInntektrequest(
        yrkesaktivitet: Yrkesaktivitet,
        request: InntektRequest,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitet.id] ?: return yrkesaktivitet.tilYrkesaktivitetDbRecord()
        val oppdatert = eksisterende.copy(inntektRequest = request)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override suspend fun oppdaterInntektData(
        yrkesaktivitet: Yrkesaktivitet,
        inntektData: InntektData,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitet.id] ?: return yrkesaktivitet.tilYrkesaktivitetDbRecord()
        val oppdatert = eksisterende.copy(inntektData = inntektData)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override suspend fun oppdaterRefusjonsdata(
        yrkesaktivitet: Yrkesaktivitet,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitet.id] ?: return yrkesaktivitet.tilYrkesaktivitetDbRecord()
        val oppdatert = eksisterende.copy(refusjonsdata = refusjonsdata)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }
}
