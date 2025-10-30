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
        sykepengegrunnlag: Sykepengegrunnlag,
        saksbehandler: Bruker,
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
            )
        storage[id] = record
        return record
    }

    override fun hentSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord? = storage[sykepengegrunnlagId]

    override fun oppdaterSykepengegrunnlag(
        sykepengegrunnlagId: UUID,
        sykepengegrunnlag: Sykepengegrunnlag?,
    ): SykepengegrunnlagDbRecord {
        val eksisterende = storage[sykepengegrunnlagId] ?: throw IllegalArgumentException("Ukjent id")
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
        val oppdatert =
            eksisterende.copy(
                sammenlikningsgrunnlag = sammenlikningsgrunnlag,
                oppdatert = Instant.now(),
            )
        storage[oppdatert.id] = oppdatert
        return oppdatert
    }

    override fun slettSykepengegrunnlag(sykepengegrunnlagId: UUID) {
        storage.remove(sykepengegrunnlagId)
    }
}
