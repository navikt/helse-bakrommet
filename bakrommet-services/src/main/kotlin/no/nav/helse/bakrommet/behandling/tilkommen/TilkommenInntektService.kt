package no.nav.helse.bakrommet.behandling.tilkommen

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.beregning.beregnUtbetaling
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import java.time.OffsetDateTime
import java.util.*

interface TilkommenInntektServiceDaoer : Beregningsdaoer

data class TilkommenInntektReferanse(
    val behandling: BehandlingReferanse,
    val tilkommenInntektId: UUID,
)

class TilkommenInntektService(
    private val db: DbDaoer<TilkommenInntektServiceDaoer>,
) {
    suspend fun hentTilkommenInntekt(
        ref: BehandlingReferanse,
    ): List<TilkommenInntektDbRecord> =
        db.nonTransactional {
            tilkommenInntektDao.hentForBehandling(ref.behandlingId)
        }

    suspend fun lagreTilkommenInntekt(
        ref: BehandlingReferanse,
        tilkommenInntekt: TilkommenInntekt,
        saksbehandler: Bruker,
    ): TilkommenInntektDbRecord =
        db.transactional {
            val behandling = behandlingDao.hentPeriode(ref, saksbehandler.erSaksbehandlerPåSaken())

            val tilkommenInntektDbRecord =
                TilkommenInntektDbRecord(
                    id = UUID.randomUUID(),
                    behandlingId = behandling.id,
                    tilkommenInntekt = tilkommenInntekt,
                    opprettet = OffsetDateTime.now(),
                    opprettetAvNavIdent = saksbehandler.navIdent,
                )

            tilkommenInntektDao.opprett(tilkommenInntektDbRecord).also {
                beregnUtbetaling(ref, saksbehandler)
            }
        }

    suspend fun slettTilkommenInntekt(
        ref: TilkommenInntektReferanse,
        saksbehandler: Bruker,
    ) {
        db.transactional {
            val behandling = behandlingDao.hentPeriode(ref.behandling, saksbehandler.erSaksbehandlerPåSaken())
            tilkommenInntektDao.slett(behandlingId = behandling.id, id = ref.tilkommenInntektId).also {
                beregnUtbetaling(ref.behandling, saksbehandler)
            }
        }
    }

    suspend fun endreTilkommenInntekt(
        ref: TilkommenInntektReferanse,
        tilkommenInntekt: TilkommenInntekt,
        saksbehandler: Bruker,
    ): TilkommenInntektDbRecord =
        db.transactional {
            val behandling = behandlingDao.hentPeriode(ref.behandling, saksbehandler.erSaksbehandlerPåSaken())
            val tilkommenInntektId = ref.tilkommenInntektId
            tilkommenInntektDao
                .hent(tilkommenInntektId)
                .also {
                    requireNotNull(it) {
                        "Fant ingen TilkommenInntekt med id $tilkommenInntektId"
                    }
                    require(behandling.id == it.behandlingId) {
                        "TilkommenInntekt med id $tilkommenInntektId hører ikke til behandling ${behandling.id}"
                    }
                }
            tilkommenInntektDao.oppdater(tilkommenInntektId, tilkommenInntekt).also {
                beregnUtbetaling(ref.behandling, saksbehandler)
            }
        }
}
