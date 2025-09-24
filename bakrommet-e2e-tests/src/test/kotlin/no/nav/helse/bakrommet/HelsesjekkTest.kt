package no.nav.helse.bakrommet

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HelsesjekkTest {
    
    @Test
    fun `helsesjekk endepunkter fungerer`() = runApplicationTest {
        client.get("/isalive").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("I'm alive", bodyAsText())
        }
        
        client.get("/isready").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("I'm ready", bodyAsText())
        }
    }

}
