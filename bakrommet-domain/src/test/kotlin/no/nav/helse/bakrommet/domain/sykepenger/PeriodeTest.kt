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

    @Test
    fun `erTilstøtendeIForkantAv returnerer true når periodene er tilstøtende`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 11),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertTrue(førstePeriode.erTilstøtendeIForkantAv(andrePeriode))
    }

    @Test
    fun `erTilstøtendeIForkantAv returnerer false når det er gap mellom periodene`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 12),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertFalse(førstePeriode.erTilstøtendeIForkantAv(andrePeriode))
    }

    @Test
    fun `erTilstøtendeIForkantAv returnerer false når periodene overlapper`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertFalse(førstePeriode.erTilstøtendeIForkantAv(andrePeriode))
    }

    @Test
    fun `erTilstøtendeIForkantAv returnerer false når andre periode er før første periode`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 11),
                tom = LocalDate.of(2024, 1, 20),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )

        assertFalse(førstePeriode.erTilstøtendeIForkantAv(andrePeriode))
    }

    @Test
    fun `erTilstøtendeIBakkantAv returnerer true når periodene er tilstøtende`() {
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 11),
                tom = LocalDate.of(2024, 1, 20),
            )
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )

        assertTrue(andrePeriode.erTilstøtendeIBakkantAv(førstePeriode))
    }

    @Test
    fun `erTilstøtendeIBakkantAv returnerer false når det er gap mellom periodene`() {
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 12),
                tom = LocalDate.of(2024, 1, 20),
            )
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )

        assertFalse(andrePeriode.erTilstøtendeIBakkantAv(førstePeriode))
    }

    @Test
    fun `erTilstøtendeIBakkantAv returnerer false når periodene overlapper`() {
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 20),
            )
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )

        assertFalse(andrePeriode.erTilstøtendeIBakkantAv(førstePeriode))
    }

    @Test
    fun `erTilstøtendeIBakkantAv returnerer false når første periode er etter andre periode`() {
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 11),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertFalse(andrePeriode.erTilstøtendeIBakkantAv(førstePeriode))
    }

    @Test
    fun `overlapperMed returnerer true når periodene overlapper delvis`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 5),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertTrue(førstePeriode.overlapperMed(andrePeriode))
        assertTrue(andrePeriode.overlapperMed(førstePeriode))
    }

    @Test
    fun `overlapperMed returnerer true når perioden overlapper seg selv`() {
        val periode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )

        assertTrue(periode.overlapperMed(periode))
    }

    @Test
    fun `overlapperMed returnerer true når periodene er identiske`() {
        val periode1 =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val periode2 =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )

        assertTrue(periode1.overlapperMed(periode2))
        assertTrue(periode2.overlapperMed(periode1))
    }

    @Test
    fun `overlapperMed returnerer true når periodene overlapper på én dag`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertTrue(førstePeriode.overlapperMed(andrePeriode))
        assertTrue(andrePeriode.overlapperMed(førstePeriode))
    }

    @Test
    fun `overlapperMed returnerer true når en periode omslutter den andre helt`() {
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

        assertTrue(ytrePeriode.overlapperMed(indrePeriode))
        assertTrue(indrePeriode.overlapperMed(ytrePeriode))
    }

    @Test
    fun `overlapperMed returnerer true når periodene overlapper med én dag inne i den andre`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 15),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 15),
                tom = LocalDate.of(2024, 1, 15),
            )

        assertTrue(førstePeriode.overlapperMed(andrePeriode))
        assertTrue(andrePeriode.overlapperMed(førstePeriode))
    }

    @Test
    fun `overlapperMed returnerer false når periodene er tilstøtende`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 11),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertFalse(førstePeriode.overlapperMed(andrePeriode))
        assertFalse(andrePeriode.overlapperMed(førstePeriode))
    }

    @Test
    fun `overlapperMed returnerer false når det er gap mellom periodene`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 15),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertFalse(førstePeriode.overlapperMed(andrePeriode))
        assertFalse(andrePeriode.overlapperMed(førstePeriode))
    }

    @Test
    fun `overlapperMed returnerer false når første periode er helt før andre periode`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 5),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertFalse(førstePeriode.overlapperMed(andrePeriode))
        assertFalse(andrePeriode.overlapperMed(førstePeriode))
    }

    @Test
    fun `overlapperMed returnerer true når periodene starter samme dag`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 10),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertTrue(førstePeriode.overlapperMed(andrePeriode))
        assertTrue(andrePeriode.overlapperMed(førstePeriode))
    }

    @Test
    fun `overlapperMed returnerer true når periodene slutter samme dag`() {
        val førstePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 20),
            )
        val andrePeriode =
            Periode(
                fom = LocalDate.of(2024, 1, 10),
                tom = LocalDate.of(2024, 1, 20),
            )

        assertTrue(førstePeriode.overlapperMed(andrePeriode))
        assertTrue(andrePeriode.overlapperMed(førstePeriode))
    }
}
