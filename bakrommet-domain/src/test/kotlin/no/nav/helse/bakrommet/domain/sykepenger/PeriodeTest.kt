package no.nav.helse.bakrommet.domain.sykepenger

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeriodeTest {
    @Test
    fun `omslutter returnerer true når perioden omslutter en annen periode helt`() {
        val ytrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            )
        val indrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertTrue(ytrePeriode.omslutter(indrePeriode))
    }

    @Test
    fun `omslutter returnerer true når periodene er identiske`() {
        val periode1 =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            )
        val periode2 =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            )

        assertTrue(periode1.omslutter(periode2))
    }

    @Test
    fun `omslutter returnerer true når indre periode starter samme dag`() {
        val ytrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            )
        val indrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertTrue(ytrePeriode.omslutter(indrePeriode))
    }

    @Test
    fun `omslutter returnerer true når indre periode slutter samme dag`() {
        val ytrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            )
        val indrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 31),
            )

        assertTrue(ytrePeriode.omslutter(indrePeriode))
    }

    @Test
    fun `omslutter returnerer true når indre periode er én dag`() {
        val ytrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            )
        val indrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 15),
                tom = LocalDate.of(2024, 1, 15),
            )

        assertTrue(ytrePeriode.omslutter(indrePeriode))
    }

    @Test
    fun `omslutter returnerer false når indre periode starter før ytre periode`() {
        val ytrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 31),
            )
        val indrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertFalse(ytrePeriode.omslutter(indrePeriode))
    }

    @Test
    fun `omslutter returnerer false når indre periode slutter etter ytre periode`() {
        val ytrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 20),
            )
        val indrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 31),
            )

        assertFalse(ytrePeriode.omslutter(indrePeriode))
    }

    @Test
    fun `omslutter returnerer false når indre periode starter én dag før ytre periode`() {
        val ytrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 2),
                tom = LocalDate.of(2024, 1, 31),
            )
        val indrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 15),
            )

        assertFalse(ytrePeriode.omslutter(indrePeriode))
    }

    @Test
    fun `omslutter returnerer false når indre periode slutter én dag etter ytre periode`() {
        val ytrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 30),
            )
        val indrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 15),
                tom = LocalDate.of(2024, 1, 31),
            )

        assertFalse(ytrePeriode.omslutter(indrePeriode))
    }

    @Test
    fun `omslutter returnerer false når periodene ikke overlapper`() {
        val periode1 =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val periode2 =
            Periode(
                fom = LocalDate.of(2024, 1, 20),
                tom = LocalDate.of(2024, 1, 31),
            )

        assertFalse(periode1.omslutter(periode2))
    }

    @Test
    fun `omslutter returnerer false når periodene er tilstøtende`() {
        val periode1 =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val periode2 =
            Periode(
                fom = LocalDate.of(2024, 1, 11),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertFalse(periode1.omslutter(periode2))
    }

    @Test
    fun `omslutter returnerer false når indre periode er helt utenfor ytre periode`() {
        val ytrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 20),
            )
        val indrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            )

        assertFalse(ytrePeriode.omslutter(indrePeriode))
    }
}
