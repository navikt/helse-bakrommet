package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BehandlingDaoFake : BehandlingDao {
    private val perioder = ConcurrentHashMap<UUID, BehandlingDbRecord>()

    override fun hentAlleBehandlinger(): List<BehandlingDbRecord> = perioder.values.toList()

    override fun finnBehandling(id: UUID): BehandlingDbRecord? = perioder[id]

    override fun finnBehandlingerForNaturligIdent(naturligIdent: NaturligIdent): List<BehandlingDbRecord> = perioder.values.filter { it.naturligIdent == naturligIdent }

    override fun finnBehandlingerForNaturligIdentSomOverlapper(
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        tom: LocalDate,
    ): List<BehandlingDbRecord> =
        perioder.values.filter {
            it.naturligIdent == naturligIdent && it.fom <= tom && it.tom >= fom
        }

    override fun endreStatus(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus)
    }

    override fun endreStatusOgIndividuellBegrunnelse(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
        individuellBegrunnelse: String?,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus, individuellBegrunnelse = individuellBegrunnelse)
    }

    override fun endreStatusOgBeslutter(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
        beslutterNavIdent: String?,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = perioder[periode.id] ?: return
        perioder[periode.id] = eksisterende.copy(status = nyStatus, beslutterNavIdent = beslutterNavIdent)
    }

    override fun opprettPeriode(periode: BehandlingDbRecord) {
        perioder[periode.id] = periode
    }

    override fun oppdaterSkjæringstidspunkt(
        behandlingId: UUID,
        skjæringstidspunkt: LocalDate,
    ) {
        val eksisterende = perioder[behandlingId] ?: return
        perioder[behandlingId] = eksisterende.copy(skjæringstidspunkt = skjæringstidspunkt)
    }

    override fun oppdaterSykepengegrunnlagId(
        behandlingId: UUID,
        sykepengegrunnlagId: UUID?,
    ) {
        val eksisterende = perioder[behandlingId] ?: return
        perioder[behandlingId] = eksisterende.copy(sykepengegrunnlagId = sykepengegrunnlagId)
    }

    override fun oppdaterRevurdertAvBehandlingId(
        behandlingId: UUID,
        revurdertAvBehandlingId: UUID,
    ) {
        val eksisterende = perioder[behandlingId] ?: return
        perioder[behandlingId] = eksisterende.copy(revurdertAvBehandlingId = revurdertAvBehandlingId)
    }
}
