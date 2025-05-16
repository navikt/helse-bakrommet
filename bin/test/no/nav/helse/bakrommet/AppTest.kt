package no.nav.helse.bakrommet

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppTest {
    @Test
    fun `starter appen og svarer på isalive`() =
        runApplicationTest {
            assertEquals(200, client.get("/isalive").status.value)
            assertEquals(200, client.get("/isready").status.value)
        }
}
