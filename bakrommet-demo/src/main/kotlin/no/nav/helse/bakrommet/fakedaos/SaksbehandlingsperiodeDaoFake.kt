package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeStatus
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SaksbehandlingsperiodeDaoFake : SaksbehandlingsperiodeDao {
    private val perioder = ConcurrentHashMap<UUID, Saksbehandlingsperiode>()

    override fun hentAlleSaksbehandlingsperioder(): List<Saksbehandlingsperiode> = perioder.values.toList()

    override fun finnSaksbehandlingsperiode(id: UUID): Saksbehandlingsperiode? = perioder[id]

    override fun finnPerioderForPerson(spilleromPersonId: String): List<Saksbehandlingsperiode> = perioder.values.filter { it.spilleromPersonId == spilleromPersonId }

    override fun finnPerioderForPersonSomOverlapper(
        spilleromPersonId: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Saksbehandlingsperiode> =
        perioder.values.filter {
            it.spilleromPersonId == spilleromPersonId && it.fom <= tom && it.tom >= fom
        }

    override fun endreStatus(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus)
    }

    override fun endreStatusOgIndividuellBegrunnelse(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
        individuellBegrunnelse: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus, individuellBegrunnelse = individuellBegrunnelse)
    }

    override fun endreStatusOgBeslutter(
        periode: Saksbehandlingsperiode,
        nyStatus: SaksbehandlingsperiodeStatus,
        beslutterNavIdent: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus, beslutterNavIdent = beslutterNavIdent)
    }

    override fun opprettPeriode(periode: Saksbehandlingsperiode) {
        perioder[periode.id] = periode
    }

    override fun oppdaterSkjæringstidspunkt(
        periodeId: UUID,
        skjæringstidspunkt: LocalDate,
    ) {
        val eksisterende = perioder[periodeId] ?: return
        perioder[periodeId] = eksisterende.copy(skjæringstidspunkt = skjæringstidspunkt)
    }

    override fun oppdaterSykepengegrunnlagId(
        periodeId: UUID,
        sykepengegrunnlagId: UUID?,
    ) {
        val eksisterende = perioder[periodeId] ?: return
        perioder[periodeId] = eksisterende.copy(sykepengegrunnlagId = sykepengegrunnlagId)
    }
}
