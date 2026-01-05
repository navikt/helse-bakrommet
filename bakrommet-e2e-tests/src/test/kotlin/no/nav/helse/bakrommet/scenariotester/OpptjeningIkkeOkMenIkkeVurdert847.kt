package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.api.dto.validering.ValideringDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.testutils.AInntekt
import no.nav.helse.bakrommet.testutils.Arbeidstaker
import no.nav.helse.bakrommet.testutils.Scenario
import no.nav.helse.bakrommet.testutils.SykAlleDager
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentValidering
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OpptjeningIkkeOkMenIkkeVurdert847 {
    @Test
    fun `ingenting gir ingenting`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("988888888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
            besluttOgGodkjenn = false,
        ).runWithApplicationTestBuilder { scenarioData ->
            val personId = scenarioData.scenario.pseudoId
            val behandlingId = scenarioData.behandling.id
            val validering = hentValidering(personId, behandlingId)

            assertEquals(emptyList(), validering)
        }
    }

    @Test
    fun `gir valideringsfeil hvis 8-2 ikke oppfylt men 8-47 ikke er vurdert`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("988888888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
            vilkår =
                listOf(
                    VilkaarsvurderingDto(
                        hovedspørsmål = "1",
                        vilkårskode = "OPPTJENING",
                        vurdering = VurderingDto.IKKE_OPPFYLT,
                        underspørsmål = listOf(),
                        notat = "",
                    ),
                ),
            besluttOgGodkjenn = false,
        ).runWithApplicationTestBuilder { scenarioData ->
            val personId = scenarioData.scenario.pseudoId
            val behandlingId = scenarioData.behandling.id
            val validering = hentValidering(personId, behandlingId)

            assertEquals(
                listOf(
                    ValideringDto(
                        id = "IKKE_OPPFYLT_8_2_IKKE_VURDERT_8_47",
                        tekst = "8-2 vurdert til ikke oppfylt, men 8-47 er ikke vurdert",
                    ),
                ),
                validering,
            )
        }
    }

    @Test
    fun `gir ikke valideringsfeil hvis 8-2 ikke oppfylt og 8-47 er vurdert`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("988888888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
            vilkår =
                listOf(
                    VilkaarsvurderingDto(
                        hovedspørsmål = "1",
                        vilkårskode = "OPPTJENING",
                        vurdering = VurderingDto.IKKE_OPPFYLT,
                        underspørsmål = listOf(),
                        notat = "",
                    ),
                    VilkaarsvurderingDto(
                        hovedspørsmål = "2",
                        vilkårskode = "SYK_INAKTIV",
                        vurdering = VurderingDto.IKKE_OPPFYLT,
                        underspørsmål = listOf(),
                        notat = "",
                    ),
                ),
            besluttOgGodkjenn = false,
        ).runWithApplicationTestBuilder { scenarioData ->
            val personId = scenarioData.scenario.pseudoId
            val behandlingId = scenarioData.behandling.id
            val validering = hentValidering(personId, behandlingId)

            assertEquals(emptyList(), validering)
        }
    }
}
