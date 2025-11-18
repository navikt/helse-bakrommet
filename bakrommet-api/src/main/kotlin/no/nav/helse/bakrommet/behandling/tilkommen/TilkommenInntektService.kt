package no.nav.helse.bakrommet.behandling.tilkommen

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import java.time.OffsetDateTime
import java.util.*

interface TilkommenInntektServiceDaoer : Beregningsdaoer

class TilkommenInntektService(
    private val db: DbDaoer<TilkommenInntektServiceDaoer>,
) {
    suspend fun hentTilkommenInntekt(
        ref: SaksbehandlingsperiodeReferanse,
    ): List<TilkommenInntektDbRecord> =
        db.nonTransactional {
            tilkommenInntektDao.hentForBehandling(ref.periodeUUID)
        }

    suspend fun lagreTilkommenInntekt(
        ref: SaksbehandlingsperiodeReferanse,
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

            tilkommenInntektDao.opprett(tilkommenInntektDbRecord)
        }

    suspend fun slettTilkommenInntekt(
        ref: SaksbehandlingsperiodeReferanse,
        tilkommenInntektId: UUID,
        saksbehandler: Bruker,
    ) {
        db.transactional {
            val behandling = behandlingDao.hentPeriode(ref, saksbehandler.erSaksbehandlerPåSaken())
            tilkommenInntektDao.slett(behandlingId = behandling.id, id = tilkommenInntektId)
        }
    }
}
