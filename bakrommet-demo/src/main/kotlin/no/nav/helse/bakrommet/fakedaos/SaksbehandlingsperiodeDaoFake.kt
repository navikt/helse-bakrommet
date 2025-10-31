package no.nav.helse.bakrommet.fakedaos

import kotlinx.coroutines.currentCoroutineContext
import no.nav.helse.bakrommet.CoroutineSessionContext
import no.nav.helse.bakrommet.hentSessionid
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeStatus
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SaksbehandlingsperiodeDaoFake : SaksbehandlingsperiodeDao {
    // Map av sessionId -> periodeId -> Saksbehandlingsperiode
    private val sessionData = ConcurrentHashMap<String, ConcurrentHashMap<UUID, Saksbehandlingsperiode>>()

    private suspend fun getSessionMap(): ConcurrentHashMap<UUID, Saksbehandlingsperiode> {
        val sessionId = hentSessionid()
        println("getter session map")
        println("2: ${currentCoroutineContext()[CoroutineSessionContext.Key]?.sessionid}")
        return sessionData.getOrPut(sessionId) { ConcurrentHashMap() }
    }

    private suspend fun getPerioder(): ConcurrentHashMap<UUID, Saksbehandlingsperiode> {
        val sessionId = hentSessionid()
        println("getter session map")
        println("2: ${currentCoroutineContext()[CoroutineSessionContext.Key]?.sessionid}")
        return sessionData.getOrPut(sessionId) { ConcurrentHashMap() }
    }

    override suspend fun hentAlleSaksbehandlingsperioder(): List<Saksbehandlingsperiode> = getPerioder().values.toList()

    override suspend fun finnSaksbehandlingsperiode(id: UUID): Saksbehandlingsperiode? = getPerioder()[id]

    override suspend fun finnPerioderForPerson(spilleromPersonId: String): List<Saksbehandlingsperiode> = getPerioder().values.filter { it.spilleromPersonId == spilleromPersonId }

    override suspend fun finnPerioderForPersonSomOverlapper(
        spilleromPersonId: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Saksbehandlingsperiode> =
        getPerioder().values.filter {
            it.spilleromPersonId == spilleromPersonId && it.fom <= tom && it.tom >= fom
        }

    override suspend fun endreStatus(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = getPerioder()[periode.id] ?: return
        getPerioder()[periode.id] = eksisterende.copy(status = nyStatus)
    }

    override suspend fun endreStatusOgIndividuellBegrunnelse(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
        individuellBegrunnelse: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = getPerioder()[periode.id] ?: return
        getPerioder()[periode.id] = eksisterende.copy(status = nyStatus, individuellBegrunnelse = individuellBegrunnelse)
    }

    override suspend fun endreStatusOgBeslutter(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
        beslutterNavIdent: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = getPerioder()[periode.id] ?: return
        getPerioder()[periode.id] = eksisterende.copy(status = nyStatus, beslutterNavIdent = beslutterNavIdent)
    }

    override suspend fun opprettPeriode(periode: Saksbehandlingsperiode) {
        getPerioder()[periode.id] = periode
    }

    override suspend fun oppdaterSkjæringstidspunkt(
        periodeId: UUID,
        skjæringstidspunkt: LocalDate?,
    ) {
        val eksisterende = getPerioder()[periodeId] ?: return
        getPerioder()[periodeId] = eksisterende.copy(skjæringstidspunkt = skjæringstidspunkt)
    }

    override suspend fun oppdaterSykepengegrunnlagId(
        periodeId: UUID,
        sykepengegrunnlagId: UUID?,
    ) {
        val eksisterende = getPerioder()[periodeId] ?: return
        getPerioder()[periodeId] = eksisterende.copy(sykepengegrunnlagId = sykepengegrunnlagId)
    }
}
