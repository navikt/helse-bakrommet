package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
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
            utbetalingsberegningTest {
                periode(fom = førsteDag, tom = førsteDag.plusDays(13))

                yrkesaktivitet {
                    id(yrkesaktivitet1Id)
                    arbeidstaker()
                    fra(førsteDag)
                    syk(grad = 100, antallDager = 14)
                }

                yrkesaktivitet {
                    id(yrkesaktivitet2Id)
                    arbeidstaker()
                    fra(førsteDag)
                    syk(grad = 50, antallDager = 14)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet1Id)
                    beløp(50000) // 50 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
                    refusjon {
                        fra(førsteDag)
                        til(førsteDag.plusDays(7))
                        beløp(50000) // 50 000 kr/mnd refusjon
                    }
                    refusjon {
                        fra(førsteDag.plusDays(8))
                        åpen()
                        beløp(10000) // 10 000 kr/mnd refusjon
                    }
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet2Id)
                    beløp(50000) // 50 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
                }
            }

        val resultat = UtbetalingsberegningLogikk.beregnAlaSpleis(input)
        println(resultat.toJsonNode().toPrettyString())
    }
}
