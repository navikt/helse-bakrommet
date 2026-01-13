package no.nav.helse.bakrommet.behandling.sykepengegrunnlag

import no.nav.helse.bakrommet.domain.Bruker
import java.util.UUID

interface SykepengegrunnlagDao {
    fun lagreSykepengegrunnlag(
        sykepengegrunnlag: SykepengegrunnlagBase?,
        saksbehandler: Bruker,
        opprettetForBehandling: UUID,
    ): SykepengegrunnlagDbRecord

    fun finnSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord?

    fun hentSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord =
        finnSykepengegrunnlag(sykepengegrunnlagId)
            ?: throw IllegalArgumentException("Fant ikke sykepengegrunnlag for id $sykepengegrunnlagId")

    fun oppdaterSykepengegrunnlag(
        sykepengegrunnlagId: UUID,
        sykepengegrunnlag: SykepengegrunnlagBase?,
    ): SykepengegrunnlagDbRecord

    fun oppdaterSammenlikningsgrunnlag(
        sykepengegrunnlagId: UUID,
        sammenlikningsgrunnlag: Sammenlikningsgrunnlag?,
    ): SykepengegrunnlagDbRecord

    fun slettSykepengegrunnlag(sykepengegrunnlagId: UUID)

    fun settLåst(sykepengegrunnlagId: UUID)
}
