package no.nav.helse.bakrommet.pdl

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.errorhandling.PersonIkkeFunnetException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PdlClientTest {
    val token = SpilleromBearerToken("test-token")

    @Test
    fun `returnerer identer`() {
        val pdlClient = PdlMock.pdlClient()
        val resp = runBlocking { pdlClient.hentIdenterFor(saksbehandlerToken = token, ident = "1234") }
        assertEquals(setOf("12345678910", "10987654321"), resp.map { it.ident }.toSet())
    }

    @Test
    fun `kaster PersonIkkeFunnetException ved ukjent ident (404)`() {
        val pdlClient = PdlMock.pdlClient()
        assertThrows<PersonIkkeFunnetException> {
            runBlocking { pdlClient.hentIdenterFor(saksbehandlerToken = token, ident = "5555") }
        }
    }

    @Test
    fun `kaster exception ved errors`() {
        val pdlClient = PdlMock.pdlClient()
        assertThrows<RuntimeException> {
            runBlocking { pdlClient.hentIdenterFor(saksbehandlerToken = token, ident = "error") }
        }
    }
}
