package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.skapSykepengegrunnlag
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse.IKKE_MINSTEINNTEKT
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse.MINSTEINNTEKT
import org.junit.jupiter.api.Test

class TjenerUnderEnHalvGMenVurdertTilOverTest {
    @Test
    fun `Tjener under en halv G og vurdert over gir inkonsistens`() {
        TjenerUnderEnHalvGMenVurdertTilOver `skal ha inkonsistens med`
            data(
                sykepengegrunnlag = skapSykepengegrunnlag(grunnlag = 50000.0, g = 110000.0),
                vurderteVilkår = vurdertVilkårMedBegrunnelse(MINSTEINNTEKT),
            )
    }

    @Test
    fun `Tjener under en halv G og vurdert under gir konsistens`() {
        TjenerUnderEnHalvGMenVurdertTilOver `skal ha konsistens med`
            data(
                sykepengegrunnlag = skapSykepengegrunnlag(grunnlag = 50000.0, g = 110000.0),
                vurderteVilkår = vurdertVilkårMedBegrunnelse(IKKE_MINSTEINNTEKT),
            )
    }

    @Test
    fun `Ingen sykepengrgrunnlag fastsatt gir konsistens`() {
        TjenerUnderEnHalvGMenVurdertTilOver `skal ha konsistens med`
            data(
                vurderteVilkår =
                    vurdertVilkårMedBegrunnelse(
                        IKKE_MINSTEINNTEKT,
                    ),
            )
    }
}
