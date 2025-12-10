package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.person.NaturligIdent
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BehandlingDaoFake : BehandlingDao {
    private val perioder = ConcurrentHashMap<UUID, Behandling>()

    override fun hentAlleBehandlinger(): List<Behandling> = perioder.values.toList()

    override fun finnBehandling(id: UUID): Behandling? = perioder[id]

    override fun finnBehandlingerForNaturligIdent(naturligIdent: NaturligIdent): List<Behandling> = perioder.values.filter { it.naturligIdent == naturligIdent }

    override fun finnBehandlingerForNaturligIdentSomOverlapper(
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Behandling> =
        perioder.values.filter {
            it.naturligIdent == naturligIdent && it.fom <= tom && it.tom >= fom
        }

    override fun endreStatus(
        periode: Behandling,
        nyStatus: BehandlingStatus,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus)
    }

    override fun endreStatusOgIndividuellBegrunnelse(
        periode: Behandling,
        nyStatus: BehandlingStatus,
        individuellBegrunnelse: String?,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus, individuellBegrunnelse = individuellBegrunnelse)
    }

    override fun endreStatusOgBeslutter(
        periode: Behandling,
        nyStatus: BehandlingStatus,
        beslutterNavIdent: String?,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
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
        perioder[behandlingId] = eksisterende.copy(revurdertAvBehandlingId = revurdertAvBehandlingId)
    }
}
