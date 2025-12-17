package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.testutils.*
import kotlin.test.Test

/**
 * Tester som demonstrerer avrundingseffekter når utbetaling fordeles mellom flere arbeidsgivere.
 *
 * Når sykepengegrunnlaget (6G) deles mellom flere yrkesaktiviteter, blir hver yrkesaktivitet
 * avrundet separat, noe som kan gi et annet totalbeløp enn om hele beløpet ble avrundet samlet.
 */
class ToArbeidsgivereAvrundingTest {
    @Test
    fun `to arbeidsgivere med lav og høy inntekt gir avrundingsgevinst`() {
        Scenario(
            listOf(
                Arbeidstaker("911111111", inntekt = SkjønnsfastsattManglendeRapportering(120000.0), dagoversikt = SykAlleDager()),
                Arbeidstaker("922222222", inntekt = SkjønnsfastsattManglendeRapportering(624168.0), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0) // 6G
            // Arbeidsgiver 1: 120000 kr/år = 461.538 kr/dag → avrundet til 462 kr/dag
            // Arbeidsgiver 2: 624168 kr/år = 2400.646 kr/dag → avrundet til 2401 kr/dag
            // Sum: 2863 kr/dag × 10 dager = 28630 kr
            `skal ha direkteutbetaling`(28630)
        }
    }

    @Test
    fun `én arbeidsgiver med full 6G får lavere utbetaling pga avrundingstap`() {
        Scenario(
            listOf(
                Arbeidstaker("911111111", inntekt = SkjønnsfastsattManglendeRapportering(744168.0), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0) // 6G (begrenset)
            // Arbeidsgiver 1: 744168 kr/år = 2862.184 kr/dag → avrundet til 2862 kr/dag
            // Sum: 2862 kr/dag × 10 dager = 28620 kr
            // 10 kr lavere enn to-arbeidsgiverscenario pga. avrundingseffekt:
            // Separat avrunding: 462 + 2401 = 2863 kr/dag
            // Samlet avrunding: 2862 kr/dag
            // Differanse: 1 kr/dag × 10 dager = 10 kr
            `skal ha direkteutbetaling`(28620)
        }
    }
}
