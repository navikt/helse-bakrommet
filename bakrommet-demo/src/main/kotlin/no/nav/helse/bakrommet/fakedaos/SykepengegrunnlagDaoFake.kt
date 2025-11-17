package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDbRecord
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SykepengegrunnlagDaoFake : SykepengegrunnlagDao {
    private val storage = ConcurrentHashMap<UUID, SykepengegrunnlagDbRecord>()

    override fun lagreSykepengegrunnlag(
        sykepengegrunnlag: Sykepengegrunnlag?,
        saksbehandler: Bruker,
        opprettetForBehandling: UUID,
    ): SykepengegrunnlagDbRecord {
        val id = UUID.randomUUID()
        val nå = Instant.now()
        val record =
            SykepengegrunnlagDbRecord(
                id = id,
                sykepengegrunnlag = sykepengegrunnlag,
                opprettetAv = saksbehandler.navIdent,
                opprettet = nå,
                oppdatert = nå,
                sammenlikningsgrunnlag = null,
                opprettetForBehandling = opprettetForBehandling,
            )
        storage[id] = record
        return record
    }

    override fun finnSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord? = storage[sykepengegrunnlagId]

    private fun SykepengegrunnlagDbRecord.verifiserIkkeLåst() {
        if (låst) throw IllegalStateException("Sykepengegrunnlag er låst og kan ikke endres")
    }

    override fun oppdaterSykepengegrunnlag(
        sykepengegrunnlagId: UUID,
        sykepengegrunnlag: Sykepengegrunnlag?,
    ): SykepengegrunnlagDbRecord {
        val eksisterende = storage[sykepengegrunnlagId] ?: throw IllegalArgumentException("Ukjent id")
        eksisterende.verifiserIkkeLåst()
        val oppdatert =
            eksisterende.copy(
                sykepengegrunnlag = sykepengegrunnlag,
                oppdatert = Instant.now(),
            )
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun oppdaterSammenlikningsgrunnlag(
        sykepengegrunnlagId: UUID,
        sammenlikningsgrunnlag: Sammenlikningsgrunnlag?,
    ): SykepengegrunnlagDbRecord {
        val eksisterende = storage[sykepengegrunnlagId] ?: throw IllegalArgumentException("Ukjent id")
        require(eksisterende.sammenlikningsgrunnlag == null)
        eksisterende.verifiserIkkeLåst()

        val oppdatert =
            eksisterende.copy(
                sammenlikningsgrunnlag = sammenlikningsgrunnlag,
                oppdatert = Instant.now(),
            )
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun slettSykepengegrunnlag(sykepengegrunnlagId: UUID) {
        storage[sykepengegrunnlagId]!!.verifiserIkkeLåst()

        storage.remove(sykepengegrunnlagId)
    }

    override fun settLåst(sykepengegrunnlagId: UUID) {
        val eksisterende = storage[sykepengegrunnlagId] ?: throw IllegalArgumentException("Ukjent id")
        eksisterende.verifiserIkkeLåst()
        val oppdatert =
            eksisterende.copy(
                oppdatert = Instant.now(),
                låst = true,
            )
        storage[oppdatert.id] = oppdatert
    }
}
