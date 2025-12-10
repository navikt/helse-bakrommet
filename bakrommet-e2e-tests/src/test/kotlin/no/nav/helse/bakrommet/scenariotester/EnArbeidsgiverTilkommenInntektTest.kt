package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.api.dto.tilkommen.OpprettTilkommenInntektRequestDto
import no.nav.helse.bakrommet.testutils.Arbeidstaker
import no.nav.helse.bakrommet.testutils.Scenario
import no.nav.helse.bakrommet.testutils.SkjønnsfastsattManglendeRapportering
import no.nav.helse.bakrommet.testutils.SykAlleDager
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentTidslinje
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentTidslinjeV2
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.leggTilTilkommenInntekt
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.slettTilkommenInntekt
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.januar
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EnArbeidsgiverTilkommenInntektTest {
    @Test
    fun `tilkommen midt utti en vanlig sak`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888888888",
                        inntekt = SkjønnsfastsattManglendeRapportering(260000.0),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
            fom = 1.januar(2021),
            tom = 10.januar(2021),
            besluttOgGodkjenn = false,
        ).runWithApplicationTestBuilder { førsteBehandling ->
            førsteBehandling.`skal ha direkteutbetaling`(6000)

            val personId = førsteBehandling.scenario.pseudoId
            val periodeId = førsteBehandling.periode.id
            val tilkommen =
                leggTilTilkommenInntekt(
                    periodeId = periodeId,
                    personId = personId,
                    tilkommenInntekt =
                        OpprettTilkommenInntektRequestDto(
                            ident = "999444555",
                            fom = 4.januar(2021),
                            tom = 10.januar(2021),
                            yrkesaktivitetType = TilkommenInntektYrkesaktivitetType.VIRKSOMHET,
                            inntektForPerioden = BigDecimal(14000),
                            notatTilBeslutter = "har jobbet og svarte dette i søknaden",
                            ekskluderteDager = emptyList(),
                        ),
                )
            hentUtbetalingsberegning(personId, periodeId).also { beregning ->
                beregning!!
                    .beregningData.spilleromOppdrag.oppdrag.size `should equal` 1

                beregning!!
                    .beregningData.spilleromOppdrag.oppdrag
                    .first()
                    .totalbeløp `should equal` 1000

                hentTidslinje(personId).also { tidslinje ->
                    tidslinje.size `should equal` 2
                }
                hentTidslinjeV2(personId).also { tidslinjeV2 ->
                    tidslinjeV2.size `should equal` 1
                    val behandlingMedTilkommen = tidslinjeV2.first { it.tilkommenInntekt.isNotEmpty() }
                    behandlingMedTilkommen.tilkommenInntekt.size `should equal` 1
                    val tilkommenInntektDto = behandlingMedTilkommen.tilkommenInntekt.first()
                    tilkommenInntektDto.id `should equal` tilkommen.id
                    tilkommenInntektDto.ident `should equal` "999444555"
                    tilkommenInntektDto.fom `should equal` 4.januar(2021)
                    tilkommenInntektDto.tom `should equal` 10.januar(2021)
                    tilkommenInntektDto.yrkesaktivitetType `should equal` no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetType.VIRKSOMHET
                }
            }
            slettTilkommenInntekt(periodeId = periodeId, personId = personId, tilkommenInntektId = tilkommen.id)
        }
    }
}
