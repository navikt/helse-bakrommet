package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering.IKKE_OPPFYLT
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering.OPPFYLT
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.OPPTJENING
import org.junit.jupiter.api.Test

class HarIkkeOpptjeningVedUtbetalingTest {
    @Test
    fun `Har ikke vurdert opptjeneing ved utbetaling gir inkonsistens`() {
        HarIkkeOpptjeningVedUtbetaling `skal ha inkonsistens med`
            data(
                yrkesaktiviteter = arbeidstaker(),
                beregningData = skapUtbetaling(25000),
            )
    }

    @Test
    fun `Har vurdert opptjening til oppfylt ved utbetaling gir konsistens`() {
        HarIkkeOpptjeningVedUtbetaling `skal ha konsistens med`
            data(
                vurderteVilkår = listOf(vilkårVurdertSom(OPPTJENING, OPPFYLT)),
                yrkesaktiviteter = arbeidstaker(),
                beregningData = skapUtbetaling(25000),
            )
    }

    @Test
    fun `Har vurdert opptjening til ikke oppfylt ved utbetaling gir inkonsistens`() {
        HarIkkeOpptjeningVedUtbetaling `skal ha inkonsistens med`
            data(
                vurderteVilkår = listOf(vilkårVurdertSom(OPPTJENING, IKKE_OPPFYLT)),
                yrkesaktiviteter = arbeidstaker(),
                beregningData = skapUtbetaling(25000),
            )
    }
}
