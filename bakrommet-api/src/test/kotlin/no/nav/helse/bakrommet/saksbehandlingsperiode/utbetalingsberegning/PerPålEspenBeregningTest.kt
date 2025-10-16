package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Beregningssporing.ARBEIDSTAKER_100
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Beregningssporing.ORDINAER_SELVSTENDIG_80
import no.nav.helse.desember
import no.nav.helse.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class PerPålEspenBeregningTest {
    @Test
    fun `Per`() {
        /**
         * Arbeidstaker: kr 1.200.000
         * Næring: kr 0
         * Total inntekt: kr 1.200.000
         * 50% syk
         * Inntektstap: kr 600.000
         * Får sykepenger: kr 300.000
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
                        `med beløp`(100000)
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
                `skal ha dekningsgrad begrunnelse`(ORDINAER_SELVSTENDIG_80)
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
                `skal ha dekningsgrad begrunnelse`(ARBEIDSTAKER_100)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(50)
                    `skal ha utbetaling`(1154)
                    `skal ha sykdoms grad`(50)
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(1)
                `oppdrag nummer`(0) {
                    `skal ha fagområde`("SP")
                    `skal ha netto beløp`(301194)
                    `skal ha total beløp`(301194)
                }
            }
        }
    }

    @Test
    fun `Pål`() {
        /**
         * Arbeidstaker: kr 600.000
         * Næring: kr 600.000
         * Total inntekt: kr 1.200.000
         * 100% syk som arbeidstaker
         * Inntektstap: kr 600.000
         * Får sykepenger: kr 600.000
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
                        `med beløp`(50000)
                    }
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    `som næringsdrivende`()
                    `fra dato`(1.januar(2024))
                    `har arbeidsdager`(365)
                    `med inntektData` {
                        `med beløp`(50000)
                    }
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetIdNæring) {
                `skal ha antall dager`(365)
                `skal ha dekningsgrad`(80)
                `skal ha dekningsgrad begrunnelse`(ORDINAER_SELVSTENDIG_80)
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
                `skal ha dekningsgrad begrunnelse`(ARBEIDSTAKER_100)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(50)
                    `skal ha utbetaling`(1154)
                    `skal ha sykdoms grad`(100)
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(1)
                `oppdrag nummer`(0) {
                    `skal ha fagområde`("SP")
                    `skal ha netto beløp`(301194) // TODO denne blir feil, skal være 600.000
                    `skal ha total beløp`(301194)
                }
            }
        }
    }

    @Test
    fun `Espen`() {
        /**
         * Arbeidstaker: kr 600.000
         * Næring: kr 600.000
         * Total inntekt: kr 1.200.000
         * 100% syk som næringsdrivende
         * Inntektstap: kr 600.000
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
                        `med beløp`(50000)
                    }
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    `som næringsdrivende`()
                    `fra dato`(1.januar(2024))
                    `er syk`(grad = 100, antallDager = 365)
                    `med inntektData` {
                        `med beløp`(50000)
                    }
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetIdNæring) {
                `skal ha antall dager`(365)
                `skal ha dekningsgrad`(80)
                `skal ha dekningsgrad begrunnelse`(ORDINAER_SELVSTENDIG_80)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(50)
                    `skal ha sykdoms grad`(100)
                    `skal ha ingen refusjon`()
                    `skal ha utbetaling`(923)
                }
            }

            `ha yrkesaktivitet`(yrkesaktivitetIdArbeidstaker) {
                `skal ha antall dager`(365)
                `skal ha dekningsgrad`(100)
                `skal ha dekningsgrad begrunnelse`(ARBEIDSTAKER_100)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(50)
                    `skal ha sykdoms grad`(0)
                    `skal ha ingen utbetaling`()
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(1)
                `oppdrag nummer`(0) {
                    `skal ha fagområde`("SP")
                    `skal ha netto beløp`(240903) // TODO denne blir feil, skal være null
                    `skal ha total beløp`(240903)
                }
            }
        }
    }
}
