package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.TypeArbeidstaker
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.testutils.Arbeidstaker
import no.nav.helse.bakrommet.testutils.Inntektsmelding
import no.nav.helse.bakrommet.testutils.Scenario
import no.nav.helse.bakrommet.testutils.ScenarioDefaults
import no.nav.helse.bakrommet.testutils.SykAlleDager
import no.nav.helse.bakrommet.testutils.lagSykedager
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettSaksbehandlingsperiode
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettYrkesaktivitet
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settDagoversikt
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settRefusjon
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.slettYrkesaktivitet
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.dto.InntektbeløpDto
import org.junit.jupiter.api.Test

class ArbeidsgiverForlengelseNyArbeidsgiverTest {
    @Test
    fun `ny periode kant i kant arver sykepengegrunnlag`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888",
                        inntekt =
                            Inntektsmelding(
                                20000.0,
                                Refusjonsperiode(
                                    ScenarioDefaults.fom,
                                    ScenarioDefaults.tom,
                                    InntektbeløpDto.MånedligDouble(15000.0),
                                ),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->

            førsteBehandling.`skal ha utbetaling`(2310)
            førsteBehandling.`skal ha refusjon`(6920, "888")
            val forrigePeriode = førsteBehandling.periode

            val personId = førsteBehandling.scenario.personId
            val fom = forrigePeriode.tom.plusDays(1)
            val tom = forrigePeriode.tom.plusDays(14)
            val nestePeriode =
                opprettSaksbehandlingsperiode(personId, fom, tom)

            hentYrkesaktiviteter(personId, nestePeriode.id).first().also {
                slettYrkesaktivitet(personId, nestePeriode.id, it.id)
            }
            val nyYrkesaktivitet =
                opprettYrkesaktivitet(
                    personId,
                    nestePeriode.id,
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "654"),
                    ),
                )
            settDagoversikt(personId, nestePeriode.id, nyYrkesaktivitet, lagSykedager(fom, tom, grad = 100))
            settRefusjon(
                personId,
                nestePeriode.id,
                nyYrkesaktivitet,
                listOf(
                    Refusjonsperiode(
                        fom,
                        tom,
                        InntektbeløpDto.MånedligDouble(4000.0),
                    ),
                ),
            )
            hentUtbetalingsberegning(personId, nestePeriode.id).also { beregning ->
                beregning!!
                    .beregningData.spilleromOppdrag.oppdrag.size `should equal` 0 // TODO når impl så skal dette være 2!
            }
        }
    }
}
