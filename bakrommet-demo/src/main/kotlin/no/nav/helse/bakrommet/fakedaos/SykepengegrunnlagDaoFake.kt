package no.nav.helse.bakrommet.fakedaos

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.hentSessionid
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDbRecord
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SykepengegrunnlagDaoFake : SykepengegrunnlagDao {
    // Map av sessionId -> sykepengegrunnlagId -> SykepengegrunnlagDbRecord
    private val sessionData = ConcurrentHashMap<String, ConcurrentHashMap<UUID, SykepengegrunnlagDbRecord>>()

    private fun getSessionMap(): ConcurrentHashMap<UUID, SykepengegrunnlagDbRecord> =
        runBlocking {
            val sessionId = hentSessionid()

            sessionData.getOrPut(sessionId) { ConcurrentHashMap() }
        }

    private val storage: ConcurrentHashMap<UUID, SykepengegrunnlagDbRecord>
        get() = getSessionMap()

    override suspend fun lagreSykepengegrunnlag(
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

    override suspend fun hentSykepengegrunnlag(sykepengegrunnlagId: UUID): SykepengegrunnlagDbRecord? = storage[sykepengegrunnlagId]

    override suspend fun oppdaterSykepengegrunnlag(
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

    override suspend fun oppdaterSammenlikningsgrunnlag(
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

    override suspend fun slettSykepengegrunnlag(sykepengegrunnlagId: UUID) {
        storage.remove(sykepengegrunnlagId)
    }
}
