package no.nav.helse.bakrommet.e2e.scenariotester

import forventOk
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.testutils.Arbeidstaker
import no.nav.helse.bakrommet.e2e.testutils.Inntektsmelding
import no.nav.helse.bakrommet.e2e.testutils.Scenario
import no.nav.helse.bakrommet.e2e.testutils.SykAlleDager
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.getVilkårsvurderinger
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandlingOgForventOk
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import org.junit.jupiter.api.Test

class VilkårKopieresTest {
    @Test
    fun `vilkår kopieres i forlengelser`() {
        Scenario(
            vilkår =
                listOf(
                    VilkaarsvurderingDto(
                        hovedspørsmål = "VILKÅR1",
                        vilkårskode = "ET_VILKÅR",
                        vurdering = VurderingDto.OPPFYLT,
                        underspørsmål = emptyList(),
                        notat = "Hei hei",
                    ),
                ),
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        etOrganisasjonsnummer(),
                        inntekt =
                            Inntektsmelding(
                                20000.0,
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->

            val forrigeBehandling = førsteBehandling.behandling

            val personId = førsteBehandling.scenario.pseudoId
            val fom = forrigeBehandling.tom.plusDays(1)
            val tom = forrigeBehandling.tom.plusDays(14)
            val nesteBehandling =
                opprettBehandlingOgForventOk(personId, fom, tom)

            getVilkårsvurderinger(personId, nesteBehandling.id)
                .forventOk()
                .let { vilkårsvurdering ->
                    vilkårsvurdering.size `should equal` 1
                    val vilkår = vilkårsvurdering[0]
                    vilkår.hovedspørsmål `should equal` "VILKÅR1"
                    vilkår.vurdering `should equal` VurderingDto.OPPFYLT
                    vilkår.notat `should equal` "Hei hei"
                }
        }
    }
}
