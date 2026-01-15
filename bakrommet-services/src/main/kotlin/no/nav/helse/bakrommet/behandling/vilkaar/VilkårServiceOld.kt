package no.nav.helse.bakrommet.behandling.vilkaar

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetServiceDaoer
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.util.logg
import java.util.*

fun String.erGyldigSomKode(): Boolean {
    // Først sjekk om det er en gyldig UUID
    val erGyldigUuid =
        try {
            UUID.fromString(this)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    if (erGyldigUuid) return true

    // Hvis ikke UUID, bruk opprinnelig regex
    val regex = "^[A-ZÆØÅ0-9_]*$".toRegex()
    return regex.matches(this)
}

class Kode(
    @JsonValue val kode: String,
) {
    init {
        if (!kode.erGyldigSomKode()) {
            throw InputValideringException("Ugyldig format på Kode")
        }
    }

    override fun toString(): String = kode
}

enum class OpprettetEllerEndret {
    OPPRETTET,
    ENDRET,
}

interface VilkårServiceDaoer :
    YrkesaktivitetServiceDaoer,
    Beregningsdaoer {
    override val behandlingDao: BehandlingDao
}

class OppdatertVilkårResultat(
    val vilkaarsvurdering: LegacyVurdertVilkår,
    val opprettetEllerEndret: OpprettetEllerEndret,
    val invalidations: List<String> = emptyList(),
)

class VilkårServiceOld(
    private val db: DbDaoer<VilkårServiceDaoer>,
    private val yrkesaktivitetService: YrkesaktivitetService,
) {
    suspend fun hentVilkårsvurderingerFor(ref: BehandlingReferanse): List<LegacyVurdertVilkår> =
        db.nonTransactional {
            val periode = behandlingDao.hentPeriode(ref, krav = null, måVæreUnderBehandling = false)
            vurdertVilkårDao.hentVilkårsvurderinger(periode.id)
        }

    suspend fun leggTilEllerOpprettVurdertVilkår(
        ref: BehandlingReferanse,
        saksbehandlergrensesnittHovedspørsmålId: Kode,
        request: VilkaarsvurderingRequest,
        saksbehandler: Bruker,
    ): OppdatertVilkårResultat =
        db.transactional {
            val periode = behandlingDao.hentPeriode(ref, krav = saksbehandler.erSaksbehandlerPåSaken())
            val finnesFraFør = vurdertVilkårDao.eksisterer(periode, saksbehandlergrensesnittHovedspørsmålId)
            val vilkaarsvurdering =
                Vilkaarsvurdering(
                    vilkårskode = request.vilkårskode,
                    hovedspørsmål = saksbehandlergrensesnittHovedspørsmålId.kode,
                    vurdering = request.vurdering,
                    underspørsmål = request.underspørsmål,
                    notat = request.notat,
                )

            val opprettetEllerEndret =
                if (finnesFraFør) {
                    vurdertVilkårDao.oppdater(periode, saksbehandlergrensesnittHovedspørsmålId, vilkaarsvurdering)
                    OpprettetEllerEndret.ENDRET
                } else {
                    vurdertVilkårDao.leggTil(periode, saksbehandlergrensesnittHovedspørsmålId, vilkaarsvurdering)
                    OpprettetEllerEndret.OPPRETTET
                }
            val invalidations =
                try {
                    vilkaarsvurdering.håndterInaktivVilkår(
                        ref = ref,
                        yrkesaktivitetService = yrkesaktivitetService,
                        saksbehandler = saksbehandler,
                        daoer = this,
                    )
                } catch (e: Exception) {
                    logg.error("dsf", e)
                    throw e
                }

            OppdatertVilkårResultat(
                vilkaarsvurdering = vurdertVilkårDao.hentVilkårsvurdering(periode.id, saksbehandlergrensesnittHovedspørsmålId.kode)!!,
                opprettetEllerEndret = opprettetEllerEndret,
                invalidations = invalidations,
            )
        }

    suspend fun slettVilkårsvurdering(
        ref: BehandlingReferanse,
        vilkårsKode: Kode,
        saksbehandler: Bruker,
    ): Boolean =
        db.transactional {
            val periode = behandlingDao.hentPeriode(ref, krav = saksbehandler.erSaksbehandlerPåSaken())
            val numAffectedRows = vurdertVilkårDao.slettVilkårsvurdering(periode.id, vilkårsKode.kode)
            (numAffectedRows > 0)
        }
}
