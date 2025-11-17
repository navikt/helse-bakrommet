package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeStatus
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BehandlingDaoFake : BehandlingDao {
    private val perioder = ConcurrentHashMap<UUID, Behandling>()

    override fun hentAlleBehandlinger(): List<Behandling> = perioder.values.toList()

    override fun finnBehandling(id: UUID): Behandling? = perioder[id]

    override fun finnBehandlingerForPerson(spilleromPersonId: String): List<Behandling> = perioder.values.filter { it.spilleromPersonId == spilleromPersonId }

    override fun finnBehandlingerForPersonSomOverlapper(
        spilleromPersonId: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Behandling> =
        perioder.values.filter {
            it.spilleromPersonId == spilleromPersonId && it.fom <= tom && it.tom >= fom
        }

    override fun endreStatus(
        periode: Behandling,
        nyStatus: SaksbehandlingsperiodeStatus,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus)
    }

    override fun endreStatusOgIndividuellBegrunnelse(
        periode: Behandling,
        nyStatus: SaksbehandlingsperiodeStatus,
        individuellBegrunnelse: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus, individuellBegrunnelse = individuellBegrunnelse)
    }

    override fun endreStatusOgBeslutter(
        periode: Behandling,
        nyStatus: SaksbehandlingsperiodeStatus,
        beslutterNavIdent: String?,
    ) {
        check(SaksbehandlingsperiodeStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus, beslutterNavIdent = beslutterNavIdent)
    }

    override fun opprettPeriode(periode: Behandling) {
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

    override fun oppdaterRevurdertAvBehandlingId(
        behandlingId: UUID,
        revurdertAvBehandlingId: UUID,
    ) {
        val eksisterende = perioder[behandlingId] ?: return
        perioder[behandlingId] = eksisterende.copy(revurdererSaksbehandlingsperiodeId = revurdertAvBehandlingId)
    }
}
