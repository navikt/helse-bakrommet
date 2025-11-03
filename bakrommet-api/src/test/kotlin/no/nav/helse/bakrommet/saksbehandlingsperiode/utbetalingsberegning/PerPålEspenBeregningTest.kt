package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad.*
import no.nav.helse.bakrommet.BeregningskoderDekningsgrad.SELVSTENDIG_DEKNINGSGRAD_80
import no.nav.helse.desember
import no.nav.helse.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class PerPålEspenBeregningTest {
    @Test
    fun `Per`() {
        /**
         * Arbeidstaker: kr 120.000
         * Næring: kr 0
         * Total inntekt: kr 120.000
         * 50% syk
         * Inntektstap: kr 60.000
         * Får sykepenger: kr 30.000
         */
        val yrkesaktivitetIdArbeidstaker = UUID.randomUUID()
        val yrkesaktivitetIdNæring = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    `fra dato`(1.januar(2024))
                    `til dato`(30.desember(2024))
                }
                skjæringstidspunkt(1.januar(2024))

                yrkesaktivitet {
                    id(yrkesaktivitetIdArbeidstaker)
                    `som arbeidstaker`("999999999")
                    `fra dato`(1.januar(2024))
                    `er syk`(grad = 50, antallDager = 365)
                    `med inntektData` {
                        `med beløp`(10000)
                    }
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    `som næringsdrivende`()
                    `fra dato`(1.januar(2024))
                    `har arbeidsdager`(365)
                    `med inntektData` {
                        `med beløp`(0)
                    }
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetIdNæring) {
                `skal ha antall dager`(365)
                `skal ha dekningsgrad`(80)
                `skal ha dekningsgrad begrunnelse`(SELVSTENDIG_DEKNINGSGRAD_80)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(50)
                    `skal ha sykdoms grad`(0)
                    `skal ha ingen refusjon`()
                    `skal ha ingen utbetaling`()
                }
            }

            `ha yrkesaktivitet`(yrkesaktivitetIdArbeidstaker) {
                `skal ha antall dager`(365)
                `skal ha dekningsgrad`(100)
                `skal ha dekningsgrad begrunnelse`(ARBEIDSTAKER_DEKNINGSGRAD_100)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(50)
                    `skal ha utbetaling`(231)
                    `skal ha sykdoms grad`(50)
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(1)
                `oppdrag nummer`(0) {
                    `skal ha fagområde`("SP")
                    `skal ha netto beløp`(60291)
                    `skal ha total beløp`(60291)
                }
            }
        }
    }

    @Test
    fun `Pål`() {
        /**
         * Arbeidstaker: kr 60.000
         * Næring: kr 60.000
         * Total inntekt: kr 120.000
         * 100% syk som arbeidstaker
         * Inntektstap: kr 60.000
         * Får sykepenger: kr 60.000
         */
        val yrkesaktivitetIdArbeidstaker = UUID.randomUUID()
        val yrkesaktivitetIdNæring = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    `fra dato`(1.januar(2024))
                    `til dato`(30.desember(2024))
                }
                skjæringstidspunkt(1.januar(2024))

                yrkesaktivitet {
                    id(yrkesaktivitetIdArbeidstaker)
                    `som arbeidstaker`("999999999")
                    `fra dato`(1.januar(2024))
                    `er syk`(grad = 100, antallDager = 365)
                    `med inntektData` {
                        `med beløp`(5000)
                    }
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    `som næringsdrivende`()
                    `fra dato`(1.januar(2024))
                    `har arbeidsdager`(365)
                    `med inntektData` {
                        `med beløp`(5000)
                    }
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetIdNæring) {
                `skal ha antall dager`(365)
                `skal ha dekningsgrad`(80)
                `skal ha dekningsgrad begrunnelse`(SELVSTENDIG_DEKNINGSGRAD_80)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(100)
                    `skal ha sykdoms grad`(0)
                    `skal ha ingen refusjon`()
                    `skal ha ingen utbetaling`()
                }
            }

            `ha yrkesaktivitet`(yrkesaktivitetIdArbeidstaker) {
                `skal ha antall dager`(365)
                `skal ha dekningsgrad`(100)
                `skal ha dekningsgrad begrunnelse`(ARBEIDSTAKER_DEKNINGSGRAD_100)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(100)
                    `skal ha utbetaling`(231)
                    `skal ha sykdoms grad`(100)
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(1)
                `oppdrag nummer`(0) {
                    `skal ha fagområde`("SP")
                    `skal ha netto beløp`(60291) // TODO denne blir feil, skal være 60.000
                    `skal ha total beløp`(60291)
                }
            }
        }
    }

    @Test
    fun `Espen`() {
        /**
         * Arbeidstaker: kr 60.000
         * Næring: kr 60.000
         * Total inntekt: kr 120.000
         * 100% syk som næringsdrivende
         * Inntektstap: kr 60.000
         * Får sykepenger: kr 0
         */
        val yrkesaktivitetIdArbeidstaker = UUID.randomUUID()
        val yrkesaktivitetIdNæring = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    `fra dato`(1.januar(2024))
                    `til dato`(30.desember(2024))
                }
                skjæringstidspunkt(1.januar(2024))

                yrkesaktivitet {
                    id(yrkesaktivitetIdArbeidstaker)
                    `som arbeidstaker`("999999999")
                    `fra dato`(1.januar(2024))
                    `har arbeidsdager`(365)
                    `med inntektData` {
                        `med beløp`(5000)
                    }
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    `som næringsdrivende`()
                    `fra dato`(1.januar(2024))
                    `er syk`(grad = 100, antallDager = 365)
                    `med inntektData` {
                        `med beløp`(5000)
                    }
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetIdNæring) {
                `skal ha antall dager`(365)
                `skal ha dekningsgrad`(80)
                `skal ha dekningsgrad begrunnelse`(SELVSTENDIG_DEKNINGSGRAD_80)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(0)
                    `skal ha sykdoms grad`(100)
                    `skal ha ingen refusjon`()
                    `skal ha utbetaling`(0)
                }
            }

            `ha yrkesaktivitet`(yrkesaktivitetIdArbeidstaker) {
                `skal ha antall dager`(365)
                `skal ha dekningsgrad`(100)
                `skal ha dekningsgrad begrunnelse`(ARBEIDSTAKER_DEKNINGSGRAD_100)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(0)
                    `skal ha sykdoms grad`(0)
                    `skal ha ingen utbetaling`()
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(0)
            }
        }
    }
}
