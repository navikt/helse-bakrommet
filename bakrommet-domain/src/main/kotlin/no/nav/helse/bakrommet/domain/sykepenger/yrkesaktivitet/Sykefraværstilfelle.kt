package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag.SykepengegrunnlagBase
import java.time.Instant
import java.time.LocalDate

data class SykefraværstilfelleId(
    val naturligIdent: NaturligIdent,
    val skjæringstidspunkt: LocalDate,
)

class Sykefraværstilfelle private constructor(
    val id: SykefraværstilfelleId,
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
            id: SykefraværstilfelleId,
            sykepengegrunnlag: SykepengegrunnlagBase,
            sammenlikningsgrunnlag: Sammenlikningsgrunnlag,
            opprettet: Instant,
            opprettetAv: String,
            oppdatert: Instant,
            opprettetForBehandling: BehandlingId,
            låst: Boolean = false,
        ) = Sykefraværstilfelle(
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
            naturligIdent: NaturligIdent,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: SykepengegrunnlagBase,
            sammenlikningsgrunnlag: Sammenlikningsgrunnlag,
            opprettetAv: String,
            opprettetForBehandling: BehandlingId,
        ): Sykefraværstilfelle {
            val opprettet = Instant.now()
            return Sykefraværstilfelle(
                id = SykefraværstilfelleId(naturligIdent, skjæringstidspunkt),
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
