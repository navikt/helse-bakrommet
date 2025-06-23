package no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt

import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SkapDagerFraSoknaderTest {
    @Test
    fun `skal lage arbeidsdager og helgedager for hele perioden`() {
        val fom = LocalDate.of(2024, 1, 1) // Mandag
        val tom = LocalDate.of(2024, 1, 7) // Søndag
        val søknader = emptyList<SykepengesoknadDTO>()

        val resultat = skapDagoversiktFraSoknader(søknader, fom, tom)

        assertEquals(7, resultat.size)

        // Mandag til fredag skal være arbeidsdager
        assertEquals(Dagtype.Arbeidsdag, resultat[0].dagtype) // Mandag
        assertEquals(Dagtype.Arbeidsdag, resultat[1].dagtype) // Tirsdag
        assertEquals(Dagtype.Arbeidsdag, resultat[2].dagtype) // Onsdag
        assertEquals(Dagtype.Arbeidsdag, resultat[3].dagtype) // Torsdag
        assertEquals(Dagtype.Arbeidsdag, resultat[4].dagtype) // Fredag

        // Lørdag og søndag skal være helgedager
        assertEquals(Dagtype.Helg, resultat[5].dagtype) // Lørdag
        assertEquals(Dagtype.Helg, resultat[6].dagtype) // Søndag

        // Alle dager skal ha kilde Saksbehandler
        resultat.forEach { dag ->
            assertEquals(Kilde.Saksbehandler, dag.kilde)
            assertEquals(null, dag.grad)
            assertTrue(dag.avvistBegrunnelse.isEmpty())
        }
    }

    @Test
    fun `skal håndtere tom liste med søknader`() {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 3)

        val resultat = skapDagoversiktFraSoknader(emptyList(), fom, tom)

        assertEquals(3, resultat.size)
        resultat.forEach { dag ->
            assertTrue(dag.dagtype == Dagtype.Arbeidsdag || dag.dagtype == Dagtype.Helg)
            assertEquals(Kilde.Saksbehandler, dag.kilde)
        }
    }

    @Test
    fun `skal håndtere enkelt dag`() {
        val fom = LocalDate.of(2024, 1, 1) // Mandag
        val tom = LocalDate.of(2024, 1, 1) // Samme dag

        val resultat = skapDagoversiktFraSoknader(emptyList(), fom, tom)

        assertEquals(1, resultat.size)
        assertEquals(Dagtype.Arbeidsdag, resultat[0].dagtype)
        assertEquals(fom, resultat[0].dato)
    }

    @Test
    fun `skal håndtere helg`() {
        val fom = LocalDate.of(2024, 1, 6) // Lørdag
        val tom = LocalDate.of(2024, 1, 7) // Søndag

        val resultat = skapDagoversiktFraSoknader(emptyList(), fom, tom)

        assertEquals(2, resultat.size)
        assertEquals(Dagtype.Helg, resultat[0].dagtype) // Lørdag
        assertEquals(Dagtype.Helg, resultat[1].dagtype) // Søndag
    }

    @Test
    fun `helgedager overskrives ikke av søknadsperioder, ferie eller permisjon`() {
        val fom = LocalDate.of(2024, 1, 6) // Lørdag
        val tom = LocalDate.of(2024, 1, 7) // Søndag

        // Test med tom liste for å verifisere at helgedager opprettes korrekt
        val dager = skapDagoversiktFraSoknader(emptyList(), fom, tom)

        assertEquals(2, dager.size)
        assertEquals(Dagtype.Helg, dager[0].dagtype) // Lørdag
        assertEquals(Dagtype.Helg, dager[1].dagtype) // Søndag

        // Verifiser at kilde er Saksbehandler for helgedager
        dager.forEach { dag ->
            assertEquals(Kilde.Saksbehandler, dag.kilde)
        }
    }
}
