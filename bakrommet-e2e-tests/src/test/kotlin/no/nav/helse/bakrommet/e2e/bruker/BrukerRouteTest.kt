package no.nav.helse.bakrommet.e2e.bruker

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.*
import io.ktor.client.statement.*
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.Rolle
import no.nav.helse.bakrommet.e2e.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.`should contain`
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import no.nav.helse.bakrommet.objectMapper
import org.junit.jupiter.api.Test

class BrukerRouteTest {
    @Test
    fun `les og beslutter`() {
        runApplicationTest {
            val response =
                client.get("/v1/bruker") {
                    bearerAuth(oAuthMock.token(grupper = listOf("GRUPPE_LES", "B", "C", "GRUPPE_BESLUTTER")))
                }

            val bruker: Bruker = objectMapper.readValue<Bruker>(response.bodyAsText())

            bruker.roller.`should contain`(Rolle.LES)
            bruker.roller.`should contain`(Rolle.BESLUTTER)
            bruker.roller.size `should equal` 2
        }
    }

    @Test
    fun `bare saksbehandler`() {
        runApplicationTest {
            val response =
                client.get("/v1/bruker") {
                    bearerAuth(oAuthMock.token(grupper = listOf("B", "C", "GRUPPE_SAKSBEHANDLER")))
                }

            val bruker: Bruker = objectMapper.readValue<Bruker>(response.bodyAsText())

            bruker.roller.`should contain`(Rolle.SAKSBEHANDLER)
            bruker.roller.size `should equal` 1
        }
    }

    @Test
    fun `bare les`() {
        runApplicationTest {
            val response =
                client.get("/v1/bruker") {
                    bearerAuth(oAuthMock.token(grupper = listOf("GRUPPE_LES", "B", "C")))
                }

            val bruker: Bruker = objectMapper.readValue<Bruker>(response.bodyAsText())

            bruker.roller.`should contain`(Rolle.LES)
            bruker.roller.size `should equal` 1
        }
    }
}
