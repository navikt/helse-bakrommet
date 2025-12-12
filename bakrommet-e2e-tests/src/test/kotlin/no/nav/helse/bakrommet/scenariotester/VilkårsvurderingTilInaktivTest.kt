package no.nav.helse.bakrommet.scenariotester

import io.ktor.http.HttpStatusCode
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingUnderspørsmålDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.PeriodetypeDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.RefusjonsperiodeDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.testutils.Arbeidstaker
import no.nav.helse.bakrommet.testutils.Inntektsmelding
import no.nav.helse.bakrommet.testutils.Scenario
import no.nav.helse.bakrommet.testutils.ScenarioDefaults
import no.nav.helse.bakrommet.testutils.SykAlleDager
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.oppdaterVilkårsvurdering
import no.nav.helse.bakrommet.testutils.`should equal`
import org.junit.jupiter.api.Test

class VilkårsvurderingTilInaktivTest {
    @Test
    fun `ved vilkårsvurdering til inaktiv endres yrkesaktiviteten ved en yrkesaktivitet`() {
        Scenario(
            besluttOgGodkjenn = false,
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

            hentYrkesaktiviteter(førsteBehandling.scenario.pseudoId, førsteBehandling.periode.id).also { ya ->
                ya.size `should equal` 1
                (ya.first().kategorisering is YrkesaktivitetKategoriseringDto.Arbeidstaker) `should equal` true
            }

            oppdaterVilkårsvurdering(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.periode.id,
                VilkaarsvurderingDto(
                    hovedspørsmål = "VILKÅR_INAKTIV",
                    vurdering = VurderingDto.IKKE_OPPFYLT,
                    underspørsmål =
                        listOf(
                            VilkaarsvurderingUnderspørsmålDto(
                                "5635008c-b025-445c-ab6f-4e265f1f4d12",
                                "UTE_AV_ARBEID_HOVED",
                            ),
                        ),
                    notat = "Inaktiv notat",
                ),
            ).also {
                it.invalidations `should equal` listOf("utbetalingsberegning", "yrkesaktiviteter", "sykepengegrunnlag")
            }

            hentYrkesaktiviteter(førsteBehandling.scenario.pseudoId, førsteBehandling.periode.id).also { ya ->
                ya.size `should equal` 1
                (ya.first().kategorisering is YrkesaktivitetKategoriseringDto.Inaktiv) `should equal` true

                ya.first().perioder!!.type == PeriodetypeDto.VENTETID_INAKTIV
                ya.first().perioder!!.perioder.first().let {
                    it.fom `should equal` ScenarioDefaults.fom
                    it.tom `should equal` ScenarioDefaults.fom.plusDays(13)
                }
            }

            oppdaterVilkårsvurdering(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.periode.id,
                VilkaarsvurderingDto(
                    hovedspørsmål = "VILKÅR_INAKTIV",
                    vurdering = VurderingDto.IKKE_OPPFYLT,
                    underspørsmål =
                        listOf(
                            VilkaarsvurderingUnderspørsmålDto(
                                "5635008c-b025-445c-ab6f-4e265f1f4d12",
                                "I_ARBEID_UTEN_OPPTJENING",
                            ),
                        ),
                    notat = "Inaktiv notat",
                ),
                HttpStatusCode.OK,
            ).also {
                it.invalidations `should equal` listOf("utbetalingsberegning")
            }
        }
    }

    @Test
    fun `Gjør om til inaktiv ved ingen yrkesaktiviteter`() {
        Scenario(
            besluttOgGodkjenn = false,
            yrkesaktiviteter = emptyList(),
        ).runWithApplicationTestBuilder { førsteBehandling ->

            hentYrkesaktiviteter(førsteBehandling.scenario.pseudoId, førsteBehandling.periode.id).also { ya ->
                ya.size `should equal` 0
            }

            oppdaterVilkårsvurdering(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.periode.id,
                VilkaarsvurderingDto(
                    hovedspørsmål = "VILKÅR_INAKTIV",
                    vurdering = VurderingDto.IKKE_OPPFYLT,
                    underspørsmål =
                        listOf(
                            VilkaarsvurderingUnderspørsmålDto(
                                "5635008c-b025-445c-ab6f-4e265f1f4d12",
                                "UTE_AV_ARBEID_HOVED",
                            ),
                        ),
                    notat = "Inaktiv notat",
                ),
            ).also {
                it.invalidations `should equal` listOf("utbetalingsberegning", "yrkesaktiviteter", "sykepengegrunnlag")
            }

            hentYrkesaktiviteter(førsteBehandling.scenario.pseudoId, førsteBehandling.periode.id).also { ya ->
                ya.size `should equal` 1
                (ya.first().kategorisering is YrkesaktivitetKategoriseringDto.Inaktiv) `should equal` true

                ya.first().perioder!!.type == PeriodetypeDto.VENTETID_INAKTIV
                ya.first().perioder!!.perioder.first().let {
                    it.fom `should equal` ScenarioDefaults.fom
                    it.tom `should equal` ScenarioDefaults.fom.plusDays(13)
                }
            }
        }
    }
}
