package no.nav.helse.bakrommet.behandling.validering

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.BehandlingServiceDaoer
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDbRecord
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.BeregningData
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer

data class ValideringData(
    val behandlingDbRecord: BehandlingDbRecord,
    val yrkesaktiviteter: List<Yrkesaktivitet>,
    val vurderteVilkår: List<VurdertVilkår>,
    val sykepengegrunnlag: SykepengegrunnlagDbRecord?,
    val beregningData: BeregningData?,
)

class ValideringService(
    private val db: DbDaoer<BehandlingServiceDaoer>,
) {
    companion object {
        internal fun sjekkOmOk(
            data: ValideringData,
            inkluderSluttvalidering: Boolean,
        ): List<SjekkResultat> =
            alleSjekker
                .filter { inkluderSluttvalidering || !it.sluttvalidering }
                .mapNotNull { sjekk ->
                    if (sjekk.harInkonsistens(data)) {
                        SjekkResultat(id = sjekk.id, tekst = sjekk.tekst)
                    } else {
                        null
                    }
                }
    }

    suspend fun valider(
        behandlingReferanse: BehandlingReferanse,
        inkluderSluttvalidering: Boolean,
    ): List<SjekkResultat> {
        val data =
            db.transactional {
                val behandling = behandlingDao.hentPeriode(behandlingReferanse, krav = null, måVæreUnderBehandling = false)
                ValideringData(
                    behandlingDbRecord = behandling,
                    yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktiviteter(behandling),
                    vurderteVilkår = vurdertVilkårDao.hentVilkårsvurderinger(behandling.id),
                    sykepengegrunnlag = behandling.sykepengegrunnlagId?.let { sykepengegrunnlagDao.hentSykepengegrunnlag(it) },
                    beregningData = beregningDao.hentBeregning(behandling.id)?.beregningData,
                )
            }
        return sjekkOmOk(data, inkluderSluttvalidering)
    }
}
