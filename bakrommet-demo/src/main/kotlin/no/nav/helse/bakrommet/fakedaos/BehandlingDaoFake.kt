package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.repository.BehandlingRepository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus as DomainBehandlingStatus

class BehandlingDaoFake(
    private val behandlingRepository: BehandlingRepository,
) : BehandlingDao {
    override fun finnBehandling(id: UUID): BehandlingDbRecord? = behandlingRepository.finn(BehandlingId(id))?.tilDbRecord()

    override fun finnBehandlingerForNaturligIdent(naturligIdent: NaturligIdent): List<BehandlingDbRecord> = behandlingRepository.finnFor(naturligIdent).map { it.tilDbRecord() }

    override fun finnBehandlingerForNaturligIdentSomOverlapper(
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        tom: LocalDate,
    ): List<BehandlingDbRecord> =
        behandlingRepository
            .finnFor(naturligIdent)
            .filter { it.fom <= tom && it.tom >= fom }
            .map { it.tilDbRecord() }

    override fun endreStatus(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = behandlingRepository.finn(BehandlingId(periode.id)) ?: return
        behandlingRepository.lagre(eksisterende.medStatus(nyStatus.tilDomainStatus()))
    }

    override fun endreStatusOgIndividuellBegrunnelse(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
        individuellBegrunnelse: String?,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = behandlingRepository.finn(BehandlingId(periode.id)) ?: return
        behandlingRepository.lagre(
            eksisterende
                .medStatus(nyStatus.tilDomainStatus())
                .medIndividuellBegrunnelse(individuellBegrunnelse),
        )
    }

    override fun endreStatusOgBeslutter(
        periode: BehandlingDbRecord,
        nyStatus: BehandlingStatus,
        beslutterNavIdent: String?,
    ) {
        check(BehandlingStatus.erGyldigEndring(periode.status to nyStatus))
        val eksisterende = behandlingRepository.finn(BehandlingId(periode.id)) ?: return
        behandlingRepository.lagre(
            eksisterende
                .medStatus(nyStatus.tilDomainStatus())
                .medBeslutterNavIdent(beslutterNavIdent),
        )
    }

    override fun opprettPeriode(periode: BehandlingDbRecord) {
        behandlingRepository.lagre(periode.tilBehandling())
    }

    override fun oppdaterSkjæringstidspunkt(
        behandlingId: UUID,
        skjæringstidspunkt: LocalDate,
    ) {
        val eksisterende = behandlingRepository.finn(BehandlingId(behandlingId)) ?: return
        behandlingRepository.lagre(eksisterende.medSkjæringstidspunkt(skjæringstidspunkt))
    }

    override fun oppdaterSykepengegrunnlagId(
        behandlingId: UUID,
        sykepengegrunnlagId: UUID?,
    ) {
        val eksisterende = behandlingRepository.finn(BehandlingId(behandlingId)) ?: return
        behandlingRepository.lagre(eksisterende.medSykepengegrunnlagId(sykepengegrunnlagId))
    }

    override fun oppdaterRevurdertAvBehandlingId(
        behandlingId: UUID,
        revurdertAvBehandlingId: UUID,
    ) {
        val eksisterende = behandlingRepository.finn(BehandlingId(behandlingId)) ?: return
        behandlingRepository.lagre(eksisterende.medRevurdertAvBehandlingId(BehandlingId(revurdertAvBehandlingId)))
    }

    private fun BehandlingDbRecord.tilBehandling(): Behandling =
        Behandling.fraLagring(
            id = BehandlingId(id),
            naturligIdent = naturligIdent,
            opprettet = opprettet.toInstant(),
            opprettetAvNavIdent = opprettetAvNavIdent,
            opprettetAvNavn = opprettetAvNavn,
            fom = fom,
            tom = tom,
            status = status.tilDomainStatus(),
            beslutterNavIdent = beslutterNavIdent,
            skjæringstidspunkt = skjæringstidspunkt,
            individuellBegrunnelse = individuellBegrunnelse,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererBehandlingId = revurdererSaksbehandlingsperiodeId?.let { BehandlingId(it) },
            revurdertAvBehandlingId = revurdertAvBehandlingId?.let { BehandlingId(it) },
            endringer = emptyList(),
        )

    private fun Behandling.tilDbRecord(): BehandlingDbRecord =
        BehandlingDbRecord(
            id = id.value,
            naturligIdent = naturligIdent,
            opprettet = OffsetDateTime.ofInstant(opprettet, ZoneOffset.UTC),
            opprettetAvNavIdent = opprettetAvNavIdent,
            opprettetAvNavn = opprettetAvNavn,
            fom = fom,
            tom = tom,
            status = status.tilDaoStatus(),
            beslutterNavIdent = beslutterNavIdent,
            skjæringstidspunkt = skjæringstidspunkt,
            individuellBegrunnelse = individuellBegrunnelse,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererSaksbehandlingsperiodeId = revurdererBehandlingId?.value,
            revurdertAvBehandlingId = revurdertAvBehandlingId?.value,
        )

    private fun BehandlingStatus.tilDomainStatus(): DomainBehandlingStatus = DomainBehandlingStatus.valueOf(name)

    private fun DomainBehandlingStatus.tilDaoStatus(): BehandlingStatus = BehandlingStatus.valueOf(name)

    private fun Behandling.medStatus(nyStatus: DomainBehandlingStatus): Behandling =
        Behandling.fraLagring(
            id = id,
            naturligIdent = naturligIdent,
            opprettet = opprettet,
            opprettetAvNavIdent = opprettetAvNavIdent,
            opprettetAvNavn = opprettetAvNavn,
            fom = fom,
            tom = tom,
            status = nyStatus,
            beslutterNavIdent = beslutterNavIdent,
            skjæringstidspunkt = skjæringstidspunkt,
            individuellBegrunnelse = individuellBegrunnelse,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererBehandlingId = revurdererBehandlingId,
            revurdertAvBehandlingId = revurdertAvBehandlingId,
            endringer = emptyList(),
        )

    private fun Behandling.medIndividuellBegrunnelse(nyBegrunnelse: String?): Behandling =
        Behandling.fraLagring(
            id = id,
            naturligIdent = naturligIdent,
            opprettet = opprettet,
            opprettetAvNavIdent = opprettetAvNavIdent,
            opprettetAvNavn = opprettetAvNavn,
            fom = fom,
            tom = tom,
            status = status,
            beslutterNavIdent = beslutterNavIdent,
            skjæringstidspunkt = skjæringstidspunkt,
            individuellBegrunnelse = nyBegrunnelse,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererBehandlingId = revurdererBehandlingId,
            revurdertAvBehandlingId = revurdertAvBehandlingId,
            endringer = emptyList(),
        )

    private fun Behandling.medBeslutterNavIdent(nyBeslutter: String?): Behandling =
        Behandling.fraLagring(
            id = id,
            naturligIdent = naturligIdent,
            opprettet = opprettet,
            opprettetAvNavIdent = opprettetAvNavIdent,
            opprettetAvNavn = opprettetAvNavn,
            fom = fom,
            tom = tom,
            status = status,
            beslutterNavIdent = nyBeslutter,
            skjæringstidspunkt = skjæringstidspunkt,
            individuellBegrunnelse = individuellBegrunnelse,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererBehandlingId = revurdererBehandlingId,
            revurdertAvBehandlingId = revurdertAvBehandlingId,
            endringer = emptyList(),
        )

    private fun Behandling.medSkjæringstidspunkt(nySkjæringstidspunkt: LocalDate): Behandling =
        Behandling.fraLagring(
            id = id,
            naturligIdent = naturligIdent,
            opprettet = opprettet,
            opprettetAvNavIdent = opprettetAvNavIdent,
            opprettetAvNavn = opprettetAvNavn,
            fom = fom,
            tom = tom,
            status = status,
            beslutterNavIdent = beslutterNavIdent,
            skjæringstidspunkt = nySkjæringstidspunkt,
            individuellBegrunnelse = individuellBegrunnelse,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererBehandlingId = revurdererBehandlingId,
            revurdertAvBehandlingId = revurdertAvBehandlingId,
            endringer = emptyList(),
        )

    private fun Behandling.medSykepengegrunnlagId(nySykepengegrunnlagId: UUID?): Behandling =
        Behandling.fraLagring(
            id = id,
            naturligIdent = naturligIdent,
            opprettet = opprettet,
            opprettetAvNavIdent = opprettetAvNavIdent,
            opprettetAvNavn = opprettetAvNavn,
            fom = fom,
            tom = tom,
            status = status,
            beslutterNavIdent = beslutterNavIdent,
            skjæringstidspunkt = skjæringstidspunkt,
            individuellBegrunnelse = individuellBegrunnelse,
            sykepengegrunnlagId = nySykepengegrunnlagId,
            revurdererBehandlingId = revurdererBehandlingId,
            revurdertAvBehandlingId = revurdertAvBehandlingId,
            endringer = emptyList(),
        )

    private fun Behandling.medRevurdertAvBehandlingId(nyRevurdertAvBehandlingId: BehandlingId): Behandling =
        Behandling.fraLagring(
            id = id,
            naturligIdent = naturligIdent,
            opprettet = opprettet,
            opprettetAvNavIdent = opprettetAvNavIdent,
            opprettetAvNavn = opprettetAvNavn,
            fom = fom,
            tom = tom,
            status = status,
            beslutterNavIdent = beslutterNavIdent,
            skjæringstidspunkt = skjæringstidspunkt,
            individuellBegrunnelse = individuellBegrunnelse,
            sykepengegrunnlagId = sykepengegrunnlagId,
            revurdererBehandlingId = revurdererBehandlingId,
            revurdertAvBehandlingId = nyRevurdertAvBehandlingId,
            endringer = emptyList(),
        )
}
