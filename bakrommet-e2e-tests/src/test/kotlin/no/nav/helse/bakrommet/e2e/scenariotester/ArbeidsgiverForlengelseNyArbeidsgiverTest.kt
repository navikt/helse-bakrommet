package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.RefusjonsperiodeDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.e2e.testutils.*
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import org.junit.jupiter.api.Test

class ArbeidsgiverForlengelseNyArbeidsgiverTest {
    @Test
    fun `ny periode kant i kant arver sykepengegrunnlag`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "988888888",
                        inntekt =
                            Inntektsmelding(
                                beregnetInntekt = 20000.0,
                                refusjon =
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

            førsteBehandling.`skal ha direkteutbetaling`(2310)
            førsteBehandling.`skal ha refusjon`(6920, "988888888")

            førsteBehandling.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 240000.0
            val forrigePeriode = førsteBehandling.behandling

            val personId = førsteBehandling.scenario.pseudoId
            val fom = forrigePeriode.tom.plusDays(1)
            val tom = forrigePeriode.tom.plusDays(14)
            val nestePeriode =
                opprettBehandlingOgForventOk(personId, fom, tom)

            hentYrkesaktiviteter(personId, nestePeriode.id).first().also {
                slettYrkesaktivitet(personId, nestePeriode.id, it.id)
            }
            val nyYrkesaktivitet =
                opprettYrkesaktivitetOld(
                    personId,
                    nestePeriode.id,
                    YrkesaktivitetKategoriseringDto.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = "966666666"),
                    ),
                )

            settDagoversikt(personId, nestePeriode.id, nyYrkesaktivitet, lagSykedager(fom, tom, grad = 100))
            settRefusjon(
                personId = personId,
                behandlingId = nestePeriode.id,
                yrkesaktivitetId = nyYrkesaktivitet,
                refusjon =
                    listOf(
                        RefusjonsperiodeDto(
                            fom,
                            tom,
                            4000.0,
                        ),
                    ),
            )
            hentUtbetalingsberegning(personId, nestePeriode.id).also { beregning ->
                beregning!!
                    .beregningData.spilleromOppdrag.oppdrag.size `should equal` 2

                beregning.beregningData.spilleromOppdrag.oppdrag.first().also {
                    it.fagområde `should equal` "SPREF"
                    it.totalbeløp `should equal` 1850
                }
                beregning.beregningData.spilleromOppdrag.oppdrag.last().also {
                    it.fagområde `should equal` "SP"
                    it.totalbeløp `should equal` 7380
                }
            }
        }
    }
}
