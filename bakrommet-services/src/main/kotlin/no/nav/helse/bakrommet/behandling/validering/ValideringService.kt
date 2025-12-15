package no.nav.helse.bakrommet.behandling.validering

import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.BehandlingServiceDaoer
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDbRecord
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer

data class ValideringData(
    val behandling: Behandling,
    val yrkesaktiviteter: List<Yrkesaktivitet>,
    val vurderteVilkår: List<VurdertVilkår>,
    val sykepengegrunnlag: SykepengegrunnlagDbRecord?,
)

class ValideringService(
    private val db: DbDaoer<BehandlingServiceDaoer>,
) {
    companion object {
        internal fun sjekkOmOk(data: ValideringData): List<SjekkResultat> =
            alleSjekker.mapNotNull { sjekk ->
                if (sjekk.harInkonsistens(data)) {
                    SjekkResultat(id = sjekk.id, tekst = sjekk.tekst)
                } else {
                    null
                }
            }
    }

    suspend fun valider(behandlingReferanse: BehandlingReferanse): List<SjekkResultat> {
        val data =
            db.transactional {
                val behandling = behandlingDao.hentPeriode(behandlingReferanse, krav = null)
                ValideringData(
                    behandling = behandling,
                    yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktiviteter(behandling),
                    vurderteVilkår = vurdertVilkårDao.hentVilkårsvurderinger(behandling.id),
                    sykepengegrunnlag = behandling.sykepengegrunnlagId?.let { sykepengegrunnlagDao.hentSykepengegrunnlag(it) },
                )
            }
        return sjekkOmOk(data)
    }
}
