package no.nav.helse.bakrommet.behandling.vilkaar

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering

suspend fun Vilkaarsvurdering.håndterInaktivVilkår(
    ref: BehandlingReferanse,
    yrkesaktivitetService: YrkesaktivitetService,
    saksbehandler: Bruker,
    daoer: VilkårServiceDaoer,
): List<String> {
    val invalidations = mutableListOf<String>()

    if (underspørsmål.any {
            listOf(
                "UTE_AV_ARBEID_HOVED",
                "I_ARBEID_UTEN_OPPTJENING",
            ).contains(it.svar)
        }
    ) {
        invalidations.add("utbetalingsberegning") // Vi kan få endret utbetaling pga endring av deknignsgrad

        val yrkesaktiviteter = yrkesaktivitetService.hentYrkesaktivitetFor(ref)
        if (yrkesaktiviteter.size == 1) {
            val aktiviteten = yrkesaktiviteter.first()
            if (aktiviteten.kategorisering !is YrkesaktivitetKategorisering.Inaktiv) {
                yrkesaktivitetService.oppdaterKategorisering(
                    YrkesaktivitetReferanse(ref, aktiviteten.id),
                    YrkesaktivitetKategorisering.Inaktiv(),
                    saksbehandler,
                    daoer,
                )
                invalidations.add("yrkesaktiviteter")
                invalidations.add("sykepengegrunnlag")
            }
        }
    }

    return invalidations
}
