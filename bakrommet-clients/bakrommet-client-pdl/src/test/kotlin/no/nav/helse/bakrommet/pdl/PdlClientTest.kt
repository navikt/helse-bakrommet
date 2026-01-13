package no.nav.helse.bakrommet.pdl

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.domain.person.somNaturligIdent
import no.nav.helse.bakrommet.errorhandling.PersonIkkeFunnetException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PdlClientTest {
    val token = AccessToken("test-token")

    @Test
    fun `returnerer identer`() {
        val pdlClient =
            PdlMock.pdlClient(
                identTilReplyMap =
                    mapOf(
                        "12345678910" to PdlMock.pdlReply(fnr = "01010199999", aktorId = "10987654321"),
                    ),
            )
        val resp = runBlocking { pdlClient.hentIdenterFor(saksbehandlerToken = token, ident = "12345678910".somNaturligIdent()) }
        assertEquals(setOf("01010199999", "10987654321"), resp.map { it.ident }.toSet())
    }

    @Test
    fun `kaster PersonIkkeFunnetException ved ukjent ident (404)`() {
        val pdlClient =
            PdlMock.pdlClient(
                identTilReplyMap = emptyMap(),
            )
        assertThrows<PersonIkkeFunnetException> {
            runBlocking { pdlClient.hentIdenterFor(saksbehandlerToken = token, ident = "12345678910".somNaturligIdent()) }
        }
    }

    @Test
    fun `kaster exception ved errors`() {
        val pdlClient = PdlMock.pdlClient()
        assertThrows<RuntimeException> {
            runBlocking { pdlClient.hentIdenterFor(saksbehandlerToken = token, ident = "error".somNaturligIdent()) }
        }
    }
}
