package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
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
                    fra(1.januar(2024))
                    til(30.desember(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdArbeidstaker)
                    arbeidstaker("999999999")
                    fra(1.januar(2024))
                    syk(grad = 50, antallDager = 365)
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    næringsdrivende()
                    fra(1.januar(2024))
                    arbeidsdag(365)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdArbeidstaker)
                    beløp(100000)
                    kilde(Inntektskilde.INNTEKTSMELDING)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdNæring)
                    beløp(0)
                    kilde(Inntektskilde.PENSJONSGIVENDE_INNTEKT)
                }
            }

        resultat.skal {
            haYrkesaktivitet(yrkesaktivitetIdNæring) {
                harAntallDager(365)
                harDekningsgrad(80)
                harDekningsgradBegrunnelse(ORDINAER_SELVSTENDIG_80)
                dag(1.januar(2024)) {
                    harTotalGrad(50)
                    harSykdomsGrad(0)
                    harIngenRefusjon()
                    harIngenUtbetaling()
                }
            }

            haYrkesaktivitet(yrkesaktivitetIdArbeidstaker) {
                harAntallDager(365) // Hele januar
                harDekningsgrad(100)
                harDekningsgradBegrunnelse(ARBEIDSTAKER_100)
                dag(1.januar(2024)) {
                    harTotalGrad(50)
                    harUtbetaling(1154)
                    harSykdomsGrad(50)
                }
            }

            haOppdrag {
                harAntallOppdrag(1)
                oppdrag(0) {
                    harFagområde("SP")
                    harNettoBeløp(301194)
                    harTotalbeløp(301194)
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
                    fra(1.januar(2024))
                    til(30.desember(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdArbeidstaker)
                    arbeidstaker("999999999")
                    fra(1.januar(2024))
                    syk(grad = 100, antallDager = 365)
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    næringsdrivende()
                    fra(1.januar(2024))
                    arbeidsdag(365)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdArbeidstaker)
                    beløp(50000)
                    kilde(Inntektskilde.INNTEKTSMELDING)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdNæring)
                    beløp(50000)
                    kilde(Inntektskilde.PENSJONSGIVENDE_INNTEKT)
                }
            }

        resultat.skal {
            haYrkesaktivitet(yrkesaktivitetIdNæring) {
                harAntallDager(365)
                harDekningsgrad(80)
                harDekningsgradBegrunnelse(ORDINAER_SELVSTENDIG_80)
                dag(1.januar(2024)) {
                    harTotalGrad(50)
                    harSykdomsGrad(0)
                    harIngenRefusjon()
                    harIngenUtbetaling()
                }
            }

            haYrkesaktivitet(yrkesaktivitetIdArbeidstaker) {
                harAntallDager(365) // Hele januar
                harDekningsgrad(100)
                harDekningsgradBegrunnelse(ARBEIDSTAKER_100)
                dag(1.januar(2024)) {
                    harTotalGrad(50)
                    harUtbetaling(1154)
                    harSykdomsGrad(100)
                }
            }

            haOppdrag {
                harAntallOppdrag(1)
                oppdrag(0) {
                    harFagområde("SP")
                    harNettoBeløp(301194) // TODO denne blir feil, skal være 600.000
                    harTotalbeløp(301194)
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
                    fra(1.januar(2024))
                    til(30.desember(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdArbeidstaker)
                    arbeidstaker("999999999")
                    fra(1.januar(2024))
                    arbeidsdag(365)
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    næringsdrivende()
                    fra(1.januar(2024))
                    syk(grad = 100, antallDager = 365)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdArbeidstaker)
                    beløp(50000)
                    kilde(Inntektskilde.INNTEKTSMELDING)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdNæring)
                    beløp(50000)
                    kilde(Inntektskilde.PENSJONSGIVENDE_INNTEKT)
                }
            }

        resultat.skal {
            haYrkesaktivitet(yrkesaktivitetIdNæring) {
                harAntallDager(365)
                harDekningsgrad(80)
                harDekningsgradBegrunnelse(ORDINAER_SELVSTENDIG_80)
                dag(1.januar(2024)) {
                    harTotalGrad(50)
                    harSykdomsGrad(100)
                    harIngenRefusjon()
                    harUtbetaling(923)
                }
            }

            haYrkesaktivitet(yrkesaktivitetIdArbeidstaker) {
                harAntallDager(365) // Hele januar
                harDekningsgrad(100)
                harDekningsgradBegrunnelse(ARBEIDSTAKER_100)
                dag(1.januar(2024)) {
                    harTotalGrad(50)
                    harSykdomsGrad(0)
                    harIngenUtbetaling()
                }
            }

            haOppdrag {
                harAntallOppdrag(1)
                oppdrag(0) {
                    harFagområde("SP")
                    harNettoBeløp(240903) // TODO denne blir feil, skal være null
                    harTotalbeløp(240903)
                }
            }
        }
    }
}
