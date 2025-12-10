package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.RefusjonsperiodeDto
import no.nav.helse.bakrommet.testutils.Arbeidstaker
import no.nav.helse.bakrommet.testutils.Inntektsmelding
import no.nav.helse.bakrommet.testutils.Scenario
import no.nav.helse.bakrommet.testutils.ScenarioDefaults
import no.nav.helse.bakrommet.testutils.SykAlleDager
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentVilkårsvurdering
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettBehandling
import no.nav.helse.bakrommet.testutils.`should equal`
import org.junit.jupiter.api.Test

class VilkårKopieresTest {
    @Test
    fun `vilkår kopieres i forlengelser`() {
        Scenario(
            vilkår =
                listOf(
                    VilkaarsvurderingDto(
                        hovedspørsmål = "VILKÅR1",
                        vurdering = VurderingDto.OPPFYLT,
                        underspørsmål = emptyList(),
                        notat = "Hei hei",
                    ),
                ),
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888",
                        inntekt =
                            Inntektsmelding(
                                20000.0,
                                RefusjonsperiodeDto(
                                    ScenarioDefaults.fom,
                                    ScenarioDefaults.tom,
                                    15000.0,
                                ),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->

            val forrigePeriode = førsteBehandling.periode

            val personId = førsteBehandling.scenario.pseudoId
            val fom = forrigePeriode.tom.plusDays(1)
            val tom = forrigePeriode.tom.plusDays(14)
            val nestePeriode =
                opprettBehandling(personId, fom, tom)

            hentVilkårsvurdering(personId, nestePeriode.id).let { vilkårsvurdering ->
                vilkårsvurdering.size `should equal` 1
                val vilkår = vilkårsvurdering[0]
                vilkår.hovedspørsmål `should equal` "VILKÅR1"
                vilkår.vurdering `should equal` VurderingDto.OPPFYLT
                vilkår.notat `should equal` "Hei hei"
            }
        }
    }
}
