package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.skapSykepengegrunnlag
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse.IKKE_MINSTEINNTEKT
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse.MINSTEINNTEKT
import org.junit.jupiter.api.Test

class TjenerOverEnHalvGMenVurdertTilUnderTest {
    @Test
    fun `Tjener over en halv G og vurdert over gir konsistens`() {
        TjenerOverEnHalvGMenVurdertTilUnder `skal ha konsistens med`
            data(
                sykepengegrunnlag = skapSykepengegrunnlag(grunnlag = 70000.0, g = 110000.0),
                vurderteVilkår = vurdertVilkårMedBegrunnelse(MINSTEINNTEKT),
            )
    }

    @Test
    fun `Tjener over en halv G og vurdert under gir inkonsistens`() {
        TjenerOverEnHalvGMenVurdertTilUnder `skal ha inkonsistens med`
            data(
                sykepengegrunnlag = skapSykepengegrunnlag(grunnlag = 70000.0, g = 110000.0),
                vurderteVilkår = vurdertVilkårMedBegrunnelse(IKKE_MINSTEINNTEKT),
            )
    }

    @Test
    fun `Vurdert under uten fastsatt sykepengrgrunnlaggir konsistens`() {
        TjenerOverEnHalvGMenVurdertTilUnder `skal ha konsistens med`
            data(
                vurderteVilkår = vurdertVilkårMedBegrunnelse(IKKE_MINSTEINNTEKT),
            )
    }
}
