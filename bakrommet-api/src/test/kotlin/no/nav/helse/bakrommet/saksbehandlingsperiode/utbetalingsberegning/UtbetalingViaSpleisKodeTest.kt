package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter
import no.nav.helse.bakrommet.util.toJsonNode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class UtbetalingViaSpleisKodeTest {
    @Test
    fun `to yrkesaktiviteter`() {
        val førsteDag = LocalDate.of(2024, 1, 1)
        val yrkesaktivitet1Id = UUID.randomUUID()
        val yrkesaktivitet2Id = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(førsteDag)
                    `til dato`(førsteDag.plusDays(13))
                }

                yrkesaktivitet {
                    id(yrkesaktivitet1Id)
                    `som arbeidstaker`()
                    this.`fra dato`(førsteDag)
                    `er syk`(grad = 100, antallDager = 14)
                }

                yrkesaktivitet {
                    id(yrkesaktivitet2Id)
                    `som arbeidstaker`()
                    this.`fra dato`(førsteDag)
                    `er syk`(grad = 50, antallDager = 14)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet1Id)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `fra kilde`(Inntektskilde.AINNTEKT)
                    `med refusjon` {
                        `fra dato`(førsteDag)
                        `til dato`(førsteDag.plusDays(7))
                        `med beløp`(50000) // 50 000 kr/mnd refusjon
                    }
                    `med refusjon` {
                        `fra dato`(førsteDag.plusDays(8))
                        `er åpen`()
                        `med beløp`(10000) // 10 000 kr/mnd refusjon
                    }
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet2Id)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `fra kilde`(Inntektskilde.AINNTEKT)
                }
            }

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)
        println(resultat.toJsonNode().toPrettyString())
    }
}
