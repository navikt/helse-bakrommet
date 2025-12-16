package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.enkelBehandling
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDbRecord
import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VilkaarsvurderingUnderspørsmål
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal infix fun ValideringSjekk.`skal ha inkonsistens med`(data: ValideringData) {
    val inkonsistens = harInkonsistens(data)
    assertTrue(inkonsistens, "Forventet at sjekken $id skulle være inkonsistent, men var konsistent")
}

internal infix fun ValideringSjekk.`skal ha konsistens med`(data: ValideringData) {
    val inkonsistens = harInkonsistens(data)
    assertFalse(inkonsistens, "Forventet at sjekken $id skulle være konsistent, men var inkonsistent")
}

fun vurdertVilkårMedBegrunnelse(b: VilkårskodeBegrunnelse): List<VurdertVilkår> =
    listOf(
        VurdertVilkår(
            kode = "1",
            vurdering =
                Vilkaarsvurdering(
                    vilkårskode = "WHATEVER",
                    hovedspørsmål = "1",
                    vurdering = Vurdering.OPPFYLT,
                    underspørsmål =
                        listOf(
                            VilkaarsvurderingUnderspørsmål(
                                spørsmål = "SPM_1",
                                svar = b.name,
                            ),
                        ),
                    notat = "",
                ),
        ),
    )

fun data(
    sykepengegrunnlag: SykepengegrunnlagDbRecord? = null,
    behandling: Behandling = enkelBehandling,
    yrkesaktiviteter: List<Yrkesaktivitet> = emptyList(),
    vurderteVilkår: List<VurdertVilkår> = emptyList(),
): ValideringData =
    ValideringData(
        behandling = behandling,
        yrkesaktiviteter = yrkesaktiviteter,
        sykepengegrunnlag = sykepengegrunnlag,
        vurderteVilkår = vurderteVilkår,
    )
