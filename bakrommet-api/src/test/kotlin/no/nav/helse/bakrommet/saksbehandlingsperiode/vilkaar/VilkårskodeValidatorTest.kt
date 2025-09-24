package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

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
}
