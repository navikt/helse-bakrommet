package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.PeriodetypeDto
import no.nav.helse.bakrommet.testutils.*
import no.nav.helse.bakrommet.testutils.prettyprint.prettyPrint
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Test

class ImportererArbeidsgiverperiodeFraInntektsmeldingTest {
    @Test
    fun `ny periode kant i kant arver sykepengegrunnlag`() {
        Scenario(
            tom = ScenarioDefaults.fom.plusDays(28),
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888",
                        inntekt =
                            Inntektsmelding(
                                20000.0,
                                arbeidsgiverperioder = listOf(
                                    Periode(
                                        ScenarioDefaults.fom,
                                        ScenarioDefaults.fom.plusDays(15),
                                    ),
                                ),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->

            førsteBehandling.`skal ha direkteutbetaling`(9230)
            førsteBehandling.`skal ha refusjon`(0, "888")
            førsteBehandling.yrkesaktiviteter.first().perioder!!.also {
                it.type `should equal` PeriodetypeDto.ARBEIDSGIVERPERIODE
                it.perioder.size `should equal` 1
                it.perioder.first().also { perioden ->
                    perioden.fom `should equal` ScenarioDefaults.fom
                    perioden.tom `should equal` ScenarioDefaults.fom.plusDays(15)
                }
            }
        }
    }
}
