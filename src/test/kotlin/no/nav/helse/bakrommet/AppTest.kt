package no.nav.helse.bakrommet

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.db.TestcontainersDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppTest {
    @Test
    fun `starter appen`() =
        testApplication {
            application {
                settOppKtor(instansierDatabase(TestcontainersDatabase().dbModuleConfiguration))
            }
            val response = client.get("/antallBehandlinger")
            assertEquals(200, response.status.value)
            assertEquals("0", response.bodyAsText())
        }
}
