package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.tilYrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.tilYrkesaktivitetDbRecord
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
        perioder: no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Perioder?,
        inntektData: InntektData?,
        refusjonsdata: List<no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Refusjonsperiode>?,
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

    override fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord? = storage[id]

    override fun hentYrkesaktivitet(id: UUID): Yrkesaktivitet? = storage[id]?.tilYrkesaktivitet()

    override fun hentYrkesaktiviteter(periode: Saksbehandlingsperiode): List<Yrkesaktivitet> = storage.values.filter { it.saksbehandlingsperiodeId == periode.id }.map { it.tilYrkesaktivitet() }

    override fun hentYrkesaktiviteterDbRecord(periode: Saksbehandlingsperiode): List<YrkesaktivitetDbRecord> = storage.values.filter { it.saksbehandlingsperiodeId == periode.id }

    override fun oppdaterKategoriseringOgSlettInntektData(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        kategorisering: YrkesaktivitetKategorisering,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitetDbRecord.id] ?: return yrkesaktivitetDbRecord
        val oppdatert = eksisterende.copy(kategorisering = kategorisering)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterDagoversikt(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        oppdatertDagoversikt: List<Dag>,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitetDbRecord.id] ?: return yrkesaktivitetDbRecord
        val oppdatert = eksisterende.copy(dagoversikt = oppdatertDagoversikt)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterPerioder(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        perioder: no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Perioder?,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitetDbRecord.id] ?: return yrkesaktivitetDbRecord
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
        val eksisterende = storage[yrkesaktivitet.id] ?: return yrkesaktivitet.tilYrkesaktivitetDbRecord()
        val oppdatert = eksisterende.copy(inntektRequest = request)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterInntektData(
        yrkesaktivitet: Yrkesaktivitet,
        inntektData: InntektData,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitet.id] ?: return yrkesaktivitet.tilYrkesaktivitetDbRecord()
        val oppdatert = eksisterende.copy(inntektData = inntektData)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterRefusjonsdata(
        yrkesaktivitet: Yrkesaktivitet,
        refusjonsdata: List<no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord {
        val eksisterende = storage[yrkesaktivitet.id] ?: return yrkesaktivitet.tilYrkesaktivitetDbRecord()
        val oppdatert = eksisterende.copy(refusjonsdata = refusjonsdata)
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }
}
