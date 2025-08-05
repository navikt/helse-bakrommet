package no.nav.helse.bakrommet.pdl

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.errorhandling.PersonIkkeFunnetException
import no.nav.helse.bakrommet.pdl.PdlMock.pdlClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PdlClientTest {
    val token = SpilleromBearerToken(TestOppsett.userToken)

    @Test
    fun `returnerer identer`() {
        val resp = runBlocking { pdlClient.hentIdenterFor(saksbehandlerToken = token, ident = "1234") }
        assertEquals(setOf("12345678910", "10987654321"), resp.map { it.ident }.toSet())
    }

    @Test
    fun `kaster PersonIkkeFunnetException ved ukjent ident (404)`() {
        assertThrows<PersonIkkeFunnetException> {
            runBlocking { pdlClient.hentIdenterFor(saksbehandlerToken = token, ident = "5555") }
        }
    }

    @Test
    fun `kaster exception ved errors`() {
        assertThrows<RuntimeException> {
            runBlocking { pdlClient.hentIdenterFor(saksbehandlerToken = token, ident = "error") }
        }
    }
}
