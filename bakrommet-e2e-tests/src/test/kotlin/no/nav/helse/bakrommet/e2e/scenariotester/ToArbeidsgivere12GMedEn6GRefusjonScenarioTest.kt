package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.Grunnbeløp
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.RefusjonsperiodeDto
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.testutils.Arbeidstaker
import no.nav.helse.bakrommet.e2e.testutils.Inntektsmelding
import no.nav.helse.bakrommet.e2e.testutils.Scenario
import no.nav.helse.bakrommet.e2e.testutils.ScenarioDefaults
import no.nav.helse.bakrommet.e2e.testutils.SykAlleDager
import kotlin.test.Test

class ToArbeidsgivere12GMedEn6GRefusjonScenarioTest {
    @Test
    fun `to arbeidsgivere med 6g hvor den ene refunderer 6g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        val organisasjonsnummer = etOrganisasjonsnummer()
        val etAnnetOrganisasjonsnummer = etOrganisasjonsnummer()
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        organisasjonsnummer,
                        inntekt =
                            Inntektsmelding(
                                beregnetInntekt = seksG.månedlig,
                                refusjon =
                                    RefusjonsperiodeDto(
                                        ScenarioDefaults.fom,
                                        null,
                                        seksG.dtoMånedligDouble().beløp,
                                    ),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(etAnnetOrganisasjonsnummer, inntekt = Inntektsmelding(seksG.månedlig), dagoversikt = SykAlleDager()),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(28620, orgnummer = organisasjonsnummer)
            `skal ha refusjon`(0, orgnummer = etAnnetOrganisasjonsnummer)
            `skal ha direkteutbetaling`(0)
        }
    }

    @Test
    fun `to arbeidsgivere med 6g hvor den ene refunderer 9g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        val organisasjonsnummer = etOrganisasjonsnummer()
        val etAnnetOrganisasjonsnummer = etOrganisasjonsnummer()
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        organisasjonsnummer,
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(
                                    ScenarioDefaults.fom,
                                    null,
                                    seksG.times(1.5).dtoMånedligDouble().beløp,
                                ),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(etAnnetOrganisasjonsnummer, inntekt = Inntektsmelding(seksG.månedlig), dagoversikt = SykAlleDager()),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(28620, orgnummer = organisasjonsnummer)
            `skal ha refusjon`(0, orgnummer = etAnnetOrganisasjonsnummer)
            `skal ha direkteutbetaling`(0)
        }
    }

    @Test
    fun `to arbeidsgivere med 6g hvor den ene refunderer 6g den andre 3g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        val organisasjonsnummer = etOrganisasjonsnummer()
        val etAnnetOrganisasjonsnummer = etOrganisasjonsnummer()
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        organisasjonsnummer,
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(
                        etAnnetOrganisasjonsnummer,
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.div(2).dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(19080, orgnummer = organisasjonsnummer)
            `skal ha refusjon`(9540, orgnummer = etAnnetOrganisasjonsnummer)
            `skal ha direkteutbetaling`(0)
        }
    }

    @Test
    fun `to arbeidsgivere med 6g hvor den ene refunderer 9g og den andre 3g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        val organisasjonsnummer = etOrganisasjonsnummer()
        val etAnnetOrganisasjonsnummer = etOrganisasjonsnummer()
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        organisasjonsnummer,
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(
                                    ScenarioDefaults.fom,
                                    null,
                                    seksG.times(1.5).dtoMånedligDouble().beløp,
                                ),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(
                        etAnnetOrganisasjonsnummer,
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.div(2).dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(19080, orgnummer = organisasjonsnummer)
            `skal ha refusjon`(9540, orgnummer = etAnnetOrganisasjonsnummer)
            `skal ha direkteutbetaling`(0)
        }
    }

    @Test
    fun `to arbeidsgivere med 1 9g og en 6g hvor den ene refunderer 9g og den andre 3g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        val organisasjonsnummer = etOrganisasjonsnummer()
        val etAnnetOrganisasjonsnummer = etOrganisasjonsnummer()
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        organisasjonsnummer,
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig.times(1.5),
                                emptyList(),
                                RefusjonsperiodeDto(
                                    ScenarioDefaults.fom,
                                    null,
                                    seksG.times(1.5).dtoMånedligDouble().beløp,
                                ),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(
                        etAnnetOrganisasjonsnummer,
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.div(2).dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(21470, orgnummer = organisasjonsnummer)
            `skal ha refusjon`(7160, orgnummer = etAnnetOrganisasjonsnummer)
            `skal ha direkteutbetaling`(0)
        }
    }
}
