package no.nav.helse.bakrommet.scenariotester

import io.ktor.http.*
import no.nav.helse.bakrommet.behandling.tilkommen.OpprettTilkommenInntektRequest
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.testutils.Arbeidstaker
import no.nav.helse.bakrommet.testutils.Scenario
import no.nav.helse.bakrommet.testutils.SkjønnsfastsattManglendeRapportering
import no.nav.helse.bakrommet.testutils.SykAlleDager
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.leggTilTilkommenInntekt
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.januar
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EnArbeidsgiverTilkommenInntektTest {
    @Test
    fun `tilkommen midt utti en vanlig sak`() {
        Scenario(
            fom = 1.januar(2021),
            tom = 10.januar(2021),
            besluttOgGodkjenn = false,
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888",
                        inntekt = SkjønnsfastsattManglendeRapportering(260000.0),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->
            førsteBehandling.`skal ha utbetaling`(6000)

            val personId = førsteBehandling.scenario.personId
            val periodeId = førsteBehandling.periode.id
            leggTilTilkommenInntekt(
                periodeId = periodeId,
                personId = personId,
                tilkommenInntekt =
                    OpprettTilkommenInntektRequest(
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
            }
        }
    }
}
