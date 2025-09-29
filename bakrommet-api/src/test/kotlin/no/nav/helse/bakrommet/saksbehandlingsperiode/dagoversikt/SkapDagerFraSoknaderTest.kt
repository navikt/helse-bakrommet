package no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt

import no.nav.helse.flex.sykepengesoknad.kafka.FravarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.FravarstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SelvstendigNaringsdrivendeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.flex.sykepengesoknad.kafka.VentetidDTO
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

        // Arbeidsdager skal ha kilde Saksbehandler, helgedager skal ha kilde null
        resultat.forEach { dag ->
            if (dag.dagtype == Dagtype.Helg) {
                assertEquals(null, dag.kilde)
            } else {
                assertEquals(Kilde.Saksbehandler, dag.kilde)
            }
            assertEquals(null, dag.grad)
            assertTrue(dag.avslåttBegrunnelse!!.isEmpty())
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
            if (dag.dagtype == Dagtype.Helg) {
                assertEquals(null, dag.kilde)
            } else {
                assertEquals(Kilde.Saksbehandler, dag.kilde)
            }
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

        // Verifiser at kilde er null for helgedager
        dager.forEach { dag ->
            assertEquals(null, dag.kilde)
        }
    }

    @Test
    fun `skal sette sykedager fra søknadsperioder`() {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 10)
        val søknadFom = LocalDate.of(2024, 1, 2)
        val søknadTom = LocalDate.of(2024, 1, 5)
        val grad = 80

        val søknad =
            lagSøknad(
                fom = søknadFom,
                tom = søknadTom,
                grad = grad,
            )

        val resultat = skapDagoversiktFraSoknader(listOf(søknad), fom, tom)

        // Verifiser at dagene i søknadsperioden er sykedager
        (2..5).forEach { dag ->
            val dagen = resultat.find { it.dato == LocalDate.of(2024, 1, dag) }!!
            assertEquals(Dagtype.Syk, dagen.dagtype)
            assertEquals(grad, dagen.grad)
            assertEquals(Kilde.Søknad, dagen.kilde)
        }

        // Verifiser at dager utenfor er arbeidsdager
        assertEquals(Dagtype.Arbeidsdag, resultat.find { it.dato == LocalDate.of(2024, 1, 1) }!!.dagtype)
        assertEquals(Dagtype.Arbeidsdag, resultat.find { it.dato == LocalDate.of(2024, 1, 8) }!!.dagtype)

        // Verifiser at helgen forblir helg
        assertEquals(Dagtype.Helg, resultat.find { it.dato == LocalDate.of(2024, 1, 6) }!!.dagtype)
        assertEquals(Dagtype.Helg, resultat.find { it.dato == LocalDate.of(2024, 1, 7) }!!.dagtype)
    }

    @Test
    fun `skal håndtere ferie fra søknad`() {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 10)
        val ferieFom = LocalDate.of(2024, 1, 3)
        val ferieTom = LocalDate.of(2024, 1, 5)

        val søknad =
            lagSøknad(
                fom = fom,
                tom = tom,
                ferie = listOf(Pair(ferieFom, ferieTom)),
            )

        val resultat = skapDagoversiktFraSoknader(listOf(søknad), fom, tom)

        // Verifiser at feriedagene er korrekte
        (3..5).forEach { dag ->
            val dagen = resultat.find { it.dato == LocalDate.of(2024, 1, dag) }!!
            assertEquals(Dagtype.Ferie, dagen.dagtype)
            assertEquals(Kilde.Søknad, dagen.kilde)
        }

        // Verifiser at andre dager ikke er ferie
        assertEquals(Dagtype.Syk, resultat.find { it.dato == LocalDate.of(2024, 1, 2) }!!.dagtype)
    }

    @Test
    fun `skal håndtere permisjon fra søknad`() {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 10)
        val permisjonFom = LocalDate.of(2024, 1, 4)
        val permisjonTom = LocalDate.of(2024, 1, 5)

        val søknad =
            lagSøknad(
                fom = fom,
                tom = tom,
                permisjon = listOf(Pair(permisjonFom, permisjonTom)),
            )

        val resultat = skapDagoversiktFraSoknader(listOf(søknad), fom, tom)

        // Verifiser at permisjonsdagene er korrekte
        (4..5).forEach { dag ->
            val dagen = resultat.find { it.dato == LocalDate.of(2024, 1, dag) }!!
            assertEquals(Dagtype.Permisjon, dagen.dagtype)
            assertEquals(Kilde.Søknad, dagen.kilde)
        }

        // Verifiser at andre dager ikke er permisjon
        assertEquals(Dagtype.Syk, resultat.find { it.dato == LocalDate.of(2024, 1, 3) }!!.dagtype)
    }

    @Test
    fun `ferie skal ha presedens over permisjon`() {
        val fom = LocalDate.of(2024, 1, 8)
        val tom = LocalDate.of(2024, 1, 14)
        val permisjonFom = LocalDate.of(2024, 1, 9)
        val permisjonTom = LocalDate.of(2024, 1, 12)
        val ferieFom = LocalDate.of(2024, 1, 11)
        val ferieTom = LocalDate.of(2024, 1, 12)

        val søknad =
            lagSøknad(
                fom = fom,
                tom = tom,
                permisjon = listOf(Pair(permisjonFom, permisjonTom)),
                ferie = listOf(Pair(ferieFom, ferieTom)),
            )

        val resultat = skapDagoversiktFraSoknader(listOf(søknad), fom, tom)

        // Dag før permisjon er sykedag
        assertEquals(Dagtype.Syk, resultat.find { it.dato == LocalDate.of(2024, 1, 8) }!!.dagtype)
        // Permisjonsdager som ikke overlapper med ferie
        assertEquals(Dagtype.Permisjon, resultat.find { it.dato == LocalDate.of(2024, 1, 9) }!!.dagtype)
        assertEquals(Dagtype.Permisjon, resultat.find { it.dato == LocalDate.of(2024, 1, 10) }!!.dagtype)

        // Feriedagene skal overskrive permisjonsdagene
        assertEquals(Dagtype.Ferie, resultat.find { it.dato == LocalDate.of(2024, 1, 11) }!!.dagtype)
        assertEquals(Dagtype.Ferie, resultat.find { it.dato == LocalDate.of(2024, 1, 12) }!!.dagtype)

        // Helgedagene skal forbli helg
        assertEquals(Dagtype.Helg, resultat.find { it.dato == LocalDate.of(2024, 1, 13) }!!.dagtype)
        assertEquals(Dagtype.Helg, resultat.find { it.dato == LocalDate.of(2024, 1, 14) }!!.dagtype)
    }

    @Test
    fun `skal håndtere arbeidGjenopptatt fra søknad`() {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 10)
        val arbeidGjenopptatt = LocalDate.of(2024, 1, 5)

        val søknad =
            lagSøknad(
                fom = fom,
                tom = tom,
                arbeidGjenopptatt = arbeidGjenopptatt,
            )

        val resultat = skapDagoversiktFraSoknader(listOf(søknad), fom, tom)

        // Dager før arbeidGjenopptatt skal forbli sykedager
        (1..4).forEach { dag ->
            val dagen = resultat.find { it.dato == LocalDate.of(2024, 1, dag) }!!
            assertEquals(Dagtype.Syk, dagen.dagtype)
            assertEquals(Kilde.Søknad, dagen.kilde)
        }

        // Dager fra og med arbeidGjenopptatt skal være arbeidsdager (unntatt helg)
        (5..10).forEach { dag ->
            val dagen = resultat.find { it.dato == LocalDate.of(2024, 1, dag) }!!
            if (dagen.dato.dayOfWeek.value in 6..7) {
                // Helgedager skal forbli helg
                assertEquals(Dagtype.Helg, dagen.dagtype)
                assertEquals(null, dagen.kilde)
            } else {
                // Arbeidsdager skal være arbeidsdager
                assertEquals(Dagtype.Arbeidsdag, dagen.dagtype)
                assertEquals(Kilde.Søknad, dagen.kilde)
                assertEquals(null, dagen.grad)
            }
        }
    }

    @Test
    fun `arbeidGjenopptatt skal overskrive andre dagtyper unntatt helg`() {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 10)
        val arbeidGjenopptatt = LocalDate.of(2024, 1, 6)
        val ferieFom = LocalDate.of(2024, 1, 7)
        val ferieTom = LocalDate.of(2024, 1, 8)

        val søknad =
            lagSøknad(
                fom = fom,
                tom = tom,
                ferie = listOf(Pair(ferieFom, ferieTom)),
                arbeidGjenopptatt = arbeidGjenopptatt,
            )

        val resultat = skapDagoversiktFraSoknader(listOf(søknad), fom, tom)

        // Dager før arbeidGjenopptatt skal forbli sykedager
        (1..5).forEach { dag ->
            val dagen = resultat.find { it.dato == LocalDate.of(2024, 1, dag) }!!
            assertEquals(Dagtype.Syk, dagen.dagtype)
        }

        // Fra arbeidGjenopptatt skal alle dager være arbeidsdager (unntatt helg)
        assertEquals(Dagtype.Helg, resultat.find { it.dato == LocalDate.of(2024, 1, 6) }!!.dagtype) // Lørdag
        assertEquals(Dagtype.Helg, resultat.find { it.dato == LocalDate.of(2024, 1, 7) }!!.dagtype) // Søndag
        assertEquals(Dagtype.Arbeidsdag, resultat.find { it.dato == LocalDate.of(2024, 1, 8) }!!.dagtype) // Mandag
        assertEquals(Dagtype.Arbeidsdag, resultat.find { it.dato == LocalDate.of(2024, 1, 9) }!!.dagtype) // Tirsdag
        assertEquals(Dagtype.Arbeidsdag, resultat.find { it.dato == LocalDate.of(2024, 1, 10) }!!.dagtype) // Onsdag

        // Verifiser at ferie ikke har presedens over arbeidGjenopptatt på arbeidsdager
        // Men helgedager skal forbli helg uansett
        assertEquals(Dagtype.Helg, resultat.find { it.dato == LocalDate.of(2024, 1, 6) }!!.dagtype) // Lørdag - skal være helg
        assertEquals(Dagtype.Helg, resultat.find { it.dato == LocalDate.of(2024, 1, 7) }!!.dagtype) // Søndag - skal være helg
    }


}

private fun lagSøknad(
    fom: LocalDate,
    tom: LocalDate,
    grad: Int = 100,
    ferie: List<Pair<LocalDate, LocalDate>> = emptyList(),
    permisjon: List<Pair<LocalDate, LocalDate>> = emptyList(),
    arbeidGjenopptatt: LocalDate? = null,
    ventetid: Pair<LocalDate, LocalDate>? = null,
): SykepengesoknadDTO {
    return SykepengesoknadDTO(
        id = "test-soknad",
        fnr = "12345678910",
        fom = fom,
        tom = tom,
        type = SoknadstypeDTO.ARBEIDSTAKERE,
        status = SoknadsstatusDTO.SENDT,
        arbeidGjenopptatt = arbeidGjenopptatt,
        soknadsperioder =
            listOf(
                SoknadsperiodeDTO(
                    fom = fom,
                    tom = tom,
                    grad = grad,
                    sykmeldingsgrad = grad,
                    faktiskGrad = null,
                    sykmeldingstype = null,
                    avtaltTimer = null,
                    faktiskTimer = null,
                ),
            ),
        fravar =
            ferie.map { (fom, tom) ->
                FravarDTO(
                    fom = fom,
                    tom = tom,
                    type = FravarstypeDTO.FERIE,
                )
            } +
                permisjon.map { (fom, tom) ->
                    FravarDTO(
                        fom = fom,
                        tom = tom,
                        type = FravarstypeDTO.PERMISJON,
                    )
                },
        selvstendigNaringsdrivende =
            ventetid?.let { (ventetidFom, ventetidTom) ->
                SelvstendigNaringsdrivendeDTO(
                    roller = emptyList(),
                    ventetid =
                        VentetidDTO(
                            fom = ventetidFom,
                            tom = ventetidTom,
                        ),
                )
            },
    )
}
