package no.nav.helse.bakrommet.pdl

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.errorhandling.PersonIkkeFunnetException
import no.nav.helse.bakrommet.pdl.PdlMock.pdlClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PdlClientTest {
    val token = OboToken("PDL-TOKEN")

    @Test
    fun `returnerer identer`() {
        val resp = runBlocking { pdlClient.hentIdenterFor(pdlToken = token, ident = "1234") }
        assertEquals(setOf("12345678910", "10987654321"), resp.toSet())
    }

    @Test
    fun `kaster PersonIkkeFunnetException ved ukjent ident (404)`() {
        assertThrows<PersonIkkeFunnetException> {
            runBlocking { pdlClient.hentIdenterFor(pdlToken = token, ident = "5555") }
        }
    }

    @Test
    fun `kaster exception ved errors`() {
        assertThrows<RuntimeException> {
            runBlocking { pdlClient.hentIdenterFor(pdlToken = token, ident = "error") }
        }
    }
}
