package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad.DAGPENGEMOTTAKER_DEKNINGSGRAD_100
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Test
import java.util.UUID

class ArbeidsledigBeregningTest {
    @Test
    fun `beregner utbetaling for arbeidsledig`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    `fra dato`(1.januar(2024))
                    `til dato`(31.januar(2024))
                }
                skjæringstidspunkt(1.januar(2024))

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidsledig`()
                    `fra dato`(1.januar(2024))
                    `er syk`(grad = 50, antallDager = 15)
                    `er syk`(grad = 100, antallDager = 16)
                    inntektData(
                        InntektData.Arbeidsledig(
                            omregnetÅrsinntekt = 21666.0.månedlig.dto().årlig,
                        ),
                    )
                }
            }

        resultat.sykepengegrunnlag.sykepengegrunnlag.beløp `should equal` 259992.0

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetId) {
                `skal ha antall dager`(31) // Hele januar
                `skal ha dekningsgrad`(100)
                `skal ha dekningsgrad begrunnelse`(DAGPENGEMOTTAKER_DEKNINGSGRAD_100)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(50)
                    `skal ha ingen refusjon`()
                    `skal ha utbetaling`(500)
                }
                `på dato`(16.januar(2024)) {
                    `skal ha total grad`(100)
                    `skal ha ingen refusjon`()
                    `skal ha utbetaling`(1000)
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(1)
                `oppdrag nummer` {
                    `skal ha fagområde`("SP")
                    `skal ha netto beløp`(17500)
                    `skal ha total beløp`(17500)
                }
            }
        }
    }
}
