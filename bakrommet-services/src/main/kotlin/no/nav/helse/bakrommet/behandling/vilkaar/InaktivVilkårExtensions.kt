package no.nav.helse.bakrommet.behandling.vilkaar

import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.beregning.beregnUtbetaling
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetReferanse
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetService
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.dto.PeriodeDto

suspend fun Vilkaarsvurdering.håndterInaktivVilkår(
    ref: BehandlingReferanse,
    yrkesaktivitetService: YrkesaktivitetService,
    saksbehandler: Bruker,
    daoer: VilkårServiceDaoer,
): List<String> {
    val invalidations = mutableListOf<String>()

    if (underspørsmål.any {
            listOf(
                "UTE_AV_ARBEID_SISTE_JOBB",
                "I_ARBEID_UTEN_OPPTJENING",
            ).contains(it.svar)
        }
    ) {
        invalidations.add("utbetalingsberegning") // Vi kan få endret utbetaling pga endring av deknignsgrad

        val yrkesaktiviteter = yrkesaktivitetService.hentYrkesaktivitetFor(ref)
        if (yrkesaktiviteter.size <= 1) {
            val aktiviteten =
                if (yrkesaktiviteter.isEmpty()) {
                    invalidations.add("yrkesaktiviteter")
                    invalidations.add("sykepengegrunnlag")
                    yrkesaktivitetService.opprettYrkesaktivitet(
                        ref,
                        YrkesaktivitetKategorisering.Inaktiv(),
                        saksbehandler,
                        daoer,
                    )
                } else {
                    yrkesaktiviteter.first()
                }
            if (aktiviteten.yrkesaktivitet.kategorisering !is YrkesaktivitetKategorisering.Inaktiv) {
                invalidations.add("yrkesaktiviteter")
                invalidations.add("sykepengegrunnlag")
                yrkesaktivitetService.oppdaterKategorisering(
                    YrkesaktivitetReferanse(ref, aktiviteten.yrkesaktivitet.id),
                    YrkesaktivitetKategorisering.Inaktiv(),
                    saksbehandler,
                    daoer,
                )
            }
            aktiviteten.yrkesaktivitet.dagoversikt?.sykdomstidlinje?.let { dager ->
                if (dager.isNotEmpty() && (aktiviteten.yrkesaktivitet.perioder == null || aktiviteten.yrkesaktivitet.perioder.type !== Periodetype.VENTETID_INAKTIV)) {
                    yrkesaktivitetService.oppdaterPerioder(
                        YrkesaktivitetReferanse(ref, aktiviteten.yrkesaktivitet.id),
                        Perioder(
                            type = Periodetype.VENTETID_INAKTIV,
                            listOf(
                                PeriodeDto(
                                    dager.first().dato,
                                    minOf(dager.first().dato.plusDays(13), dager.last().dato),
                                ),
                            ),
                        ),
                        saksbehandler,
                        daoer,
                    )
                }
            }
        }
        daoer.beregnUtbetaling(
            ref = ref,
            saksbehandler = saksbehandler,
        )
    }

    return invalidations
}
