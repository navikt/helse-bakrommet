package no.nav.helse.bakrommet.behandling.vilkaar

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VilkårskodeValidatorTest {
    @Test
    fun `koder uten feil tegn er gyldige`() {
        assertTrue("BOINOR".erGyldigSomKode())
        assertTrue("IKKE_INNEN_TRE_MÅNEDER".erGyldigSomKode())
        assertTrue("PARAGRAG_8_2".erGyldigSomKode())
    }

    @Test
    fun `koder med uønskede tegn er ikke gyldige`() {
        assertFalse("IKKE.PUNKTUM".erGyldigSomKode())
        assertFalse("IKKE/SKRAASTREK".erGyldigSomKode())
        assertFalse("IKKE SPACE".erGyldigSomKode())
    }

    @Test
    fun `uuid-er er gyldige`() {
        assertTrue("e40375fa-d0a5-4a68-a90a-9e145a0a63b4".erGyldigSomKode())
        assertTrue("E40375FA-D0A5-4A68-A90A-9E145A0A63B4".erGyldigSomKode())
    }

    @Test
    fun `ugyldige uuid-er godtas ikke`() {
        assertFalse("e40375fa-d0a5-4a68-a90a".erGyldigSomKode()) // For kort
        assertFalse("e40375fa-d0a5-4a68-a90a-9e145a0a63b4-ekstra".erGyldigSomKode()) // For lang
        assertFalse("e40375fa-d0a5-4a68-a90a-9e145a0a63b4-".erGyldigSomKode()) // Ekstra bindestrek
    }

    @Test
    fun `koder med små bokstaver men ikke uuid godtas ikke`() {
        assertFalse("boinor".erGyldigSomKode()) // Små bokstaver ikke tillatt hvis ikke UUID
        assertFalse("test_kode".erGyldigSomKode()) // Små bokstaver ikke tillatt hvis ikke UUID
    }
}
