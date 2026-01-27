package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag.SykepengegrunnlagBase
import java.time.Instant
import java.util.UUID

@JvmInline
value class SykefraværstilfelleVersjonId(
    val value: UUID,
)

class SykefraværstilfelleVersjon private constructor(
    val id: SykefraværstilfelleVersjonId,
    val sykepengegrunnlag: SykepengegrunnlagBase,
    val sammenlikningsgrunnlag: Sammenlikningsgrunnlag,
    val opprettet: Instant,
    val opprettetAv: String,
    val oppdatert: Instant,
    val opprettetForBehandling: BehandlingId,
    val låst: Boolean = false,
) {
    companion object {
        fun fraLagring(
            id: SykefraværstilfelleVersjonId,
            sykepengegrunnlag: SykepengegrunnlagBase,
            sammenlikningsgrunnlag: Sammenlikningsgrunnlag,
            opprettet: Instant,
            opprettetAv: String,
            oppdatert: Instant,
            opprettetForBehandling: BehandlingId,
            låst: Boolean = false,
        ) = SykefraværstilfelleVersjon(
            id = id,
            sykepengegrunnlag = sykepengegrunnlag,
            sammenlikningsgrunnlag = sammenlikningsgrunnlag,
            opprettet = opprettet,
            opprettetAv = opprettetAv,
            oppdatert = oppdatert,
            opprettetForBehandling = opprettetForBehandling,
            låst = låst,
        )

        fun nytt(
            sykepengegrunnlag: SykepengegrunnlagBase,
            sammenlikningsgrunnlag: Sammenlikningsgrunnlag,
            opprettetAv: String,
            opprettetForBehandling: BehandlingId,
        ): SykefraværstilfelleVersjon {
            val opprettet = Instant.now()
            return SykefraværstilfelleVersjon(
                id = SykefraværstilfelleVersjonId(UUID.randomUUID()),
                sykepengegrunnlag = sykepengegrunnlag,
                sammenlikningsgrunnlag = sammenlikningsgrunnlag,
                opprettet = opprettet,
                opprettetAv = opprettetAv,
                oppdatert = opprettet,
                opprettetForBehandling = opprettetForBehandling,
                låst = false,
            )
        }
    }
}
